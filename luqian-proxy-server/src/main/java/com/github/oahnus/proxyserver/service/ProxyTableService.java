package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.generate.SnowFlake;
import com.github.oahnus.proxyserver.config.ProxyTableContainer;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.mapper.ProxyTableMapper;
import com.github.oahnus.proxyserver.utils.RandomPortUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oahnus on 2020-04-27
 * 17:40.
 */
@Service
public class ProxyTableService extends BaseService<ProxyTableMapper, ProxyTable, Long> {
    private static SnowFlake snowFlake = new SnowFlake(1, 1);

    public void create(Long userId, ProxyTable formData) {
        Integer port = formData.getPort();

        if (port == null) {
            // 随机端口
            port = RandomPortUtils.getOneRandomPort();
        }

        Boolean isUsed = checkPortIsUsed(port);

        if (isUsed) {
            throw new ServiceException("端口已在使用");
        }

        ProxyTable proxyTable = new ProxyTable();
        proxyTable.setId(snowFlake.generateId());
        proxyTable.setSysUserId(userId);
        proxyTable.setCreateTime(new Date());
        proxyTable.setEnable(true);
        proxyTable.setPort(port);

        save(proxyTable);

        ProxyTableContainer.getInstance().addProxyTable(proxyTable);

        // 通知观察者
        ProxyTableContainer.getInstance().notifyObservers();
    }

    public List<ProxyTable> loadAllActive() {
        return selectList(new QueryBuilder(ProxyTable.class)
                .eq("enable", 1));
    }

    public Boolean checkPortIsUsed(Integer port) {
        List<ProxyTable> tableList = selectList(new QueryBuilder(ProxyTable.class)
                .eq("port", port)
                .eq("enable", true));
        return !CollectionUtils.isEmpty(tableList);
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

    public void removeProxyTable(Long userId, String appId, Integer port) {
        ProxyTable proxyTable = selectOne(new QueryBuilder(ProxyTable.class)
                .eq("sysUserId", userId)
                .eq("appId", appId)
                .eq("port", port));
        
        if (proxyTable == null) {
            throw new ServiceException("数据未找到");
        }
        removeById(proxyTable.getId());

        ProxyTableContainer.getInstance().removeMapping(appId, port);
    }
}
