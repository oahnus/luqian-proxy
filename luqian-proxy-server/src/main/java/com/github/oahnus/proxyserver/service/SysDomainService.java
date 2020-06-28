package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.proxyserver.entity.SysDomain;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.manager.DomainManager;
import com.github.oahnus.proxyserver.mapper.SysDomainMapper;
import com.github.oahnus.proxyserver.utils.NginxUtils;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by oahnus on 2020-06-02
 * 23:03.
 */
@Service
public class SysDomainService extends BaseService<SysDomainMapper, SysDomain, Integer> {
    public List<SysDomain> availableList() {
        return selectList(new QueryBuilder(SysDomain.class)
                .eq("enable", true));
    }

    public String addNewDomain(SysDomain sysDomain) {
        Integer port = sysDomain.getPort();
        String domain = sysDomain.getDomain();
        List<SysDomain> domains = selectList(new QueryBuilder(SysDomain.class)
                .eq("port", port)
                .or()
                .eq("domain", domain));

        if (!CollectionUtils.isEmpty(domains)) {
            throw new ServiceException("域名或端口已被占用");
        }
        save(sysDomain);
        // 添加DomainManager
        DomainManager.addDomain(sysDomain);

        try {
            return NginxUtils.generateNginxServerConfig(sysDomain);
        } catch (IOException | TemplateException e) {
            throw new ServiceException("生成配置文件失败, 请手动配置");
        }
    }

    public void changeEnableStatus(Integer id, Boolean enable) {
        SysDomain sysDomain = selectById(id);
        SysDomain activeDomain = DomainManager.getActiveDomain(sysDomain.getPort());
        if (activeDomain != null) {
            throw new ServiceException("域名正在使用中, 请稍后再操作");
        }
        if (enable) {
            DomainManager.addDomain(sysDomain);
        } else {
            DomainManager.removeDomain(sysDomain.getId());
        }
        sysDomain.setEnable(enable);
        updateById(sysDomain);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysDomain formData) {
        Integer sysDomainId = formData.getId();
        SysDomain sysDomain = selectById(sysDomainId);

        if (sysDomain == null) {
            throw new ServiceException("数据未找到");
        }

        // 检查域名是否在使用
        SysDomain activeDomain = DomainManager.getActiveDomain(sysDomain.getPort());
        if (activeDomain != null) {
            throw new ServiceException("域名正在使用中, 请稍后再操作");
        }

        Integer port = formData.getPort();
        String domain = formData.getDomain();
        List<SysDomain> domains = selectList(new QueryBuilder(SysDomain.class)
                .eq("port", port)
                .or()
                .eq("domain", domain));

        if (!CollectionUtils.isEmpty(domains)) {
            long count = domains.stream()
                    .filter(item -> !item.getId().equals(sysDomainId))
                    .count();
            if (count > 0) {
                throw new ServiceException("域名或端口已被占用");
            }
        }
        updateById(formData);

        // 刷新DomainManager
        DomainManager.replaceDomain(sysDomain);
    }

    public List<SysDomain> domainList() {
        List<SysDomain> domainList = selectAll();
        domainList.forEach(item -> {
            Integer port = item.getPort();
            item.setIsActive(DomainManager.isActive(port));
        });
        return domainList;
    }
}