package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.generate.SnowFlake;
import com.github.oahnus.luqiancommon.util.MyCollectionUtils;
import com.github.oahnus.proxyserver.config.ProxyTableContainer;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.mapper.ProxyTableMapper;
import com.github.oahnus.proxyserver.utils.RandomPortUtils;
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
        Integer port = formData.getPort();

        formData.setId(String.valueOf(snowFlake.generateId()));
        formData.setCreateTime(new Date());

        save(formData);

        ProxyTableContainer.getInstance().addProxyTable(formData);

        // 通知观察者
        ProxyTableContainer.getInstance().notifyObservers();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateInfo(ProxyTable proxyTable) {
        checkProxyTable(proxyTable);

        updateById(proxyTable);

        // 查找旧的proxyTable
        Optional<ProxyTable> optional = ProxyTableContainer.getInstance()
                .proxyTableMap()
                .values()
                .stream()
                .filter(pt -> pt.getId().equals(proxyTable.getId()))
                .findFirst();

        if (optional.isPresent()) {
            // 替换旧的配置信息
            Integer port = proxyTable.getPort();
            ProxyTable oldProxyTable = optional.get();
            if (oldProxyTable.getPort().equals(port)) {
                // 端口未改变， 替换proxyTable
                ProxyTableContainer.getInstance().proxyTableMap().put(port, proxyTable);
            } else {
                ProxyTableContainer.getInstance()
                        .removeProxyTable(oldProxyTable.getAppId(), oldProxyTable.getPort());
                ProxyTableContainer.getInstance()
                        .addProxyTable(proxyTable);
            }
        } else {
            // 代理配置之前可能为启用，修改了状态后需要将配置添加到map中
            ProxyTableContainer.getInstance().addProxyTable(proxyTable);
        }
        // 通知观察者
        ProxyTableContainer.getInstance().notifyObservers();
    }

    private void checkProxyTable(ProxyTable proxyTable) {
        Integer port = proxyTable.getPort();

        if (proxyTable.getIsRandom()) {
            // 随机端口
            port = RandomPortUtils.getOneRandomPort();
        } else {
            // 检查端口是否在有效范围内
            int minPort = 2000;
            int maxPort = 80000;

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
        Boolean isRandom = proxyTable.getIsRandom();

        String appId = proxyTable.getAppId();
        if (!isRandom) {
            // 固定端口直接移除
            Integer port = proxyTable.getPort();
            ProxyTableContainer.getInstance().removeProxyTable(appId, port);
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
        tableList.forEach(pt -> {
            if (pt.getIsRandom()) {
                // 如果是随机端口, 到Container中查找系统分配的端口号，填充到对象中
                ProxyTable proxyTable = proxyTableMap.get(pt.getId());
                if (proxyTable != null) {
                    pt.setPort(proxyTable.getPort());
                }
            }
        });
        return tableList;
    }
}
