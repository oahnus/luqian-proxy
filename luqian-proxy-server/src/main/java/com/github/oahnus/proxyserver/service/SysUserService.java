package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oahnus on 2020-04-23
 * 7:18.
 */
@Service
public class SysUserService extends BaseService<SysUserMapper, SysUser, Long> {
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    SysAccountService accountService;

    public SysUser getUserByUsername(String username) {
        return selectOne(new QueryBuilder(SysUser.class)
                .eq("username", username));
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUser(SysUser sysUser) {
        String username = sysUser.getUsername();
        List<SysUser> userList = selectList(new QueryBuilder(SysUser.class)
                .eq("username", username));

        if (!CollectionUtils.isEmpty(userList)) {
            throw new ServiceException("用户名已存在");
        }

        String password = sysUser.getPassword();
        sysUser.setPassword(passwordEncoder.encode(password));

        save(sysUser);

        // 创建账户
        accountService.createAccount(sysUser);
    }
}
