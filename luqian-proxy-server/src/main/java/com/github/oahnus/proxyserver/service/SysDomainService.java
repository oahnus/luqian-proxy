package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.proxyserver.entity.SysDomain;
import com.github.oahnus.proxyserver.mapper.SysDomainMapper;
import org.springframework.stereotype.Service;

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
}