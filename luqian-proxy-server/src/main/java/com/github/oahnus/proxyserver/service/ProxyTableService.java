package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.generate.SnowFlake;
import com.github.oahnus.luqiancommon.util.MyCollectionUtils;
import com.github.oahnus.proxyserver.config.ProxyTableContainer;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.SysDomain;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.manager.DomainManager;
import com.github.oahnus.proxyserver.manager.ServerChannelManager;
import com.github.oahnus.proxyserver.mapper.ProxyTableMapper;
import com.github.oahnus.proxyserver.utils.RandomPortUtils;
import io.netty.channel.Channel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by oahnus on 2020-04-27
 * 17:40.
 */
@Service
public class ProxyTableService extends BaseService<ProxyTableMapper, ProxyTable, String> {
    private static SnowFlake snowFlake = new SnowFlake(1, 1);

    public void create(ProxyTable formData) {
        checkProxyTable(formData);

        String appId = formData.getAppId();
        synchronized (appId.intern()) {
            List<ProxyTable> tableList = selectList(new QueryBuilder(ProxyTable.class)
                    .eq("appId", appId));

            // 检查 单个应用代理设置不能超过3个
            if (tableList.size() >= 3) {
                throw new ServiceException("当前应用代理数量超过限制");
            }

            if (formData.getIsUseDomain()) {
                // 分配域名
                SysDomain domain = DomainManager.borrowDomain(formData.getIsHttps());
                if (domain == null) {
                    throw new ServiceException("已无可用域名");
                }
                formData.setPort(domain.getPort()); // 设置域名的对外端口
            }

            formData.setId(String.valueOf(snowFlake.generateId()));
            formData.setCreateTime(new Date());

            save(formData);

            Channel bridgeChannel = ServerChannelManager.getBridgeChannel(formData.getAppId());
            boolean isClientOnline = bridgeChannel != null;

            if (formData.getEnable() && isClientOnline) {
                ProxyTableContainer.getInstance().addProxyTable(formData);
                // 通知观察者
                ProxyTableContainer.getInstance().notifyObservers(formData.getAppId());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateInfo(ProxyTable proxyTable) {
        checkProxyTable(proxyTable);

        // 如果使用域名
        Boolean isUseDomain = proxyTable.getIsUseDomain();
        Boolean enable = proxyTable.getEnable();
        if (enable && isUseDomain) {
            // 分配域名
            SysDomain domain = DomainManager.borrowDomain(proxyTable.getIsHttps());
            if (domain == null) {
                throw new ServiceException("已无可用域名");
            }
            proxyTable.setPort(domain.getPort()); // 设置域名的对外端口
        }

        updateById(proxyTable);

        // 查找旧的proxyTable
        Optional<ProxyTable> optional = ProxyTableContainer.getInstance()
                .proxyTableMap()
                .values()
                .stream()
                .filter(pt -> pt.getId().equals(proxyTable.getId()))
                .findFirst();

        if (optional.isPresent()) {
            ProxyTable oldProxyTable = optional.get();
            Integer oldPort = oldProxyTable.getPort();
            if (oldProxyTable.getIsUseDomain()) {
                // 旧代理规则如果使用了域名，需要归还域名
                DomainManager.returnDomain(oldPort);
            }
            ProxyTableContainer.getInstance()
                    .removeProxyTable(oldProxyTable.getAppId(), oldPort);
        }
        Channel bridgeChannel = ServerChannelManager.getBridgeChannel(proxyTable.getAppId());
        boolean isClientOnline = bridgeChannel != null;
        if (enable && isClientOnline) {
            ProxyTableContainer.getInstance().addProxyTable(proxyTable);
        }
        // 通知观察者
        ProxyTableContainer.getInstance().notifyObservers(proxyTable.getAppId());
    }

    private void checkProxyTable(ProxyTable proxyTable) {
        Integer port = proxyTable.getPort();

        // 如果使用域名，不作端口检查
        if (proxyTable.getIsUseDomain()) {
            proxyTable.setPort(null);
            proxyTable.setIsRandom(false);
            return;
        }

        if (proxyTable.getIsRandom()) {
            // 随机端口  20000-22000
            port = RandomPortUtils.getOneRandomPort();
        } else {
            // 检查端口是否在有效范围内 2000-20000
            int minPort = 2000;
            int maxPort = 20000;

            if (port > maxPort || port < minPort) {
                throw new ServiceException(String.format("有效端口范围[%d, %d],请重新输入", minPort, maxPort));
            }
        }

        // 检查端口是否已在数据库中存在记录
        List<ProxyTable> tableList = selectList(new QueryBuilder(ProxyTable.class)
                .eq("port", port)
                .eq("enable", true));

        String id = proxyTable.getId();
        boolean isPortExisted = tableList.stream()
                .anyMatch(pt -> !pt.getId().equals(id));

        if (isPortExisted) {
            throw new ServiceException("端口已在使用");
        }
        proxyTable.setPort(port);
    }

    public List<ProxyTable> loadAllActive() {
        return selectList(new QueryBuilder(ProxyTable.class)
                .eq("enable", 1));
    }

    public ProxyTable getActiveByPort(Integer port) {
        return selectOne(new QueryBuilder(ProxyTable.class)
                .eq("port", port)
                .eq("enable", true));
    }

    public ProxyTable getByPort(Integer port) {
        return selectOne(new QueryBuilder(ProxyTable.class)
                .eq("port", port));
    }

    public void removeProxyTable(String id, Long sysUserId) {
        ProxyTable proxyTable = selectOne(new QueryBuilder(ProxyTable.class)
                .eq("sysUserId", sysUserId)
                .eq("id", id));
        
        if (proxyTable == null) {
            throw new ServiceException("数据未找到");
        }
        removeById(proxyTable.getId());

        String appId = proxyTable.getAppId();
        Channel bridgeChannel = ServerChannelManager.getBridgeChannel(proxyTable.getAppId());
        boolean isClientOnline = bridgeChannel != null;

        if (!isClientOnline) {
            // 客户端离线时，直接删除配置
            return;
        }

        if (proxyTable.getIsUseDomain()) {
            // 归还http域名
            DomainManager.returnDomain(proxyTable.getPort());
        }

        Boolean isRandom = proxyTable.getIsRandom();

        if (!isRandom) {
            // 固定端口直接移除
            Integer port = proxyTable.getPort();
            ProxyTableContainer.getInstance().removeProxyTable(appId, port);
            ProxyTableContainer.getInstance().notifyObservers(appId);
        } else {
            Optional<ProxyTable> optional = ProxyTableContainer.getInstance()
                    .proxyTableMap()
                    .values()
                    .stream()
                    .filter(pt -> pt.getId().equals(id))
                    .findFirst();

            if (optional.isPresent()) {
                ProxyTable table = optional.get();
                ProxyTableContainer.getInstance().removeProxyTable(appId, table.getPort());
                ProxyTableContainer.getInstance().notifyObservers(appId);
            }
        }
    }

    public List<ProxyTable> findList(String appId, Long userId) {
        QueryBuilder qb = new QueryBuilder(ProxyTable.class);

        qb.eq("sysUserId", userId);
        if (!StringUtils.isEmpty(appId)) {
            qb.like("appId", appId);
        }
        Map<Integer, ProxyTable> tableMap = ProxyTableContainer.getInstance().proxyTableMap();
        Map<String, ProxyTable> proxyTableMap = MyCollectionUtils.convertList2Map(tableMap.values(), "id", String.class);
        List<ProxyTable> tableList = selectList(qb);
        for (ProxyTable pt : tableList) {
            ProxyTable activePt = proxyTableMap.get(pt.getId());
            pt.setActive(activePt != null);
            // 如果使用域名, 根据id查找对于域名信息
            if (pt.getIsUseDomain()) {
                if (activePt == null) {
                    continue;
                }
                SysDomain domain = DomainManager.getActiveDomain(activePt.getPort());
                if (domain != null) {
                    pt.setDomain(domain.getDomain());
                }
                continue;
            }
            if (pt.getIsRandom()) {
                // 如果是随机端口, 到Container中查找系统分配的端口号，填充到对象中
                ProxyTable proxyTable = proxyTableMap.get(pt.getId());
                if (proxyTable != null) {
                    pt.setPort(proxyTable.getPort());
                }
            }
        }

        return tableList;
    }

    public List<ProxyTable> findActiveList(String appId) {
        return selectList(new QueryBuilder(ProxyTable.class)
                .eq("appId", appId)
                .eq("enable", true));
    }
}
