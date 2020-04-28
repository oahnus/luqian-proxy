package com.github.oahnus.proxyserver.service;

import com.github.oahnus.proxyserver.config.security.SysUserDetails;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.exceptions.AuthException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Created by oahnus on 2020-04-27
 * 10:23.
 */
@Service
public class SessionService {
    public SysUser getCurUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
            SysUser sysUser = userDetails.getSysUser();
            sysUser.getId();
            return sysUser;
        } catch (Exception e) {
            throw new AuthException("未登录");
        }
    }
}
