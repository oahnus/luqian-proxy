package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/**
 * Created by oahnus on 2020-04-23
 * 7:18.
 */
@Service
public class SysUserService extends BaseService<SysUserMapper, SysUser, Long> {

    public SysUser getUserByUsername(String username) {
        return selectOne(new QueryBuilder(SysUser.class)
                .eq("username", username));
    }
}
