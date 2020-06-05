package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.annotations.Page;
import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.dto.PageableParams;
import com.github.oahnus.proxyserver.entity.SysVersion;
import com.github.oahnus.proxyserver.mapper.SysVersionMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oahnus on 2020-06-03
 * 11:06.
 */
@Service
public class SysVersionService extends BaseService<SysVersionMapper, SysVersion, Integer> {

    public PageInfo<SysVersion> page(PageableParams params) {
        PageHelper.startPage(params.getPageNum(), params.getPageSize());
        List<SysVersion> list = selectList(new QueryBuilder(SysVersion.class)
                .orderByDesc("date"));
        return new PageInfo<>(list);
    }

    public List<String> getVersionNumList() {
        return mapper.versionNumList();
    }
}
