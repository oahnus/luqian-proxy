package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.util.AESUtils;
import com.github.oahnus.luqiancommon.util.RandomUtils;
import com.github.oahnus.proxyserver.config.ProxyTableContainer;
import com.github.oahnus.proxyserver.entity.AppTable;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.mapper.AppTableMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by oahnus on 2020-04-28
 * 11:30.
 */
@Service
public class AppTableService extends BaseService<AppTableMapper, AppTable, Long> {

    public List<AppTable> loadAll() {
        return selectAll();
    }

    public AppTable createAppTable(Long sysUserId) {
        List<AppTable> appTableList = selectList(new QueryBuilder(AppTable.class)
                .eq("sysUserId", sysUserId));

        if (appTableList.size() >= 1) {
            throw new ServiceException("单个用户最多创建一个应用");
        }

        int maxTry = 5, i = 0;
        String appId = null, secret = null;
        // 生成appId 和 secret
        while (i < maxTry) {
            appId = RandomUtils.genNChars(8, RandomUtils.MODE.ONLY_NUMBER);
            // TODO L2 refact 检查appId是否重复
            if (ProxyTableContainer.getInstance().getAppSecret(appId) == null) {
                secret = AESUtils.encrypt(appId);
                break;
            }
            i++;
        }
        if (secret == null) {
            throw new ServiceException("密钥生成失败");
        }
        AppTable appTable = new AppTable();
        appTable.setAppId(appId);
        appTable.setAppSecret(secret);
        appTable.setSysUserId(sysUserId);
        appTable.setCreateTime(new Date());

        save(appTable);
        ProxyTableContainer.getInstance().addApplication(appId, secret);

        return appTable;
    }

    public List<AppTable> listByUserId(Long userId) {
        List<AppTable> appTableList = selectList(new QueryBuilder(AppTable.class)
                .eq("sysUserId", userId));
        return appTableList;
    }
}
