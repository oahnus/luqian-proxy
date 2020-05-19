package com.github.oahnus.proxyserver.config.security;

import com.alibaba.fastjson.JSON;
import com.github.oahnus.luqiancommon.dto.RespData;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by oahnus on 2020-04-26
 * 18:40.
 */
public class GoAuthSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        // 登录成功后, 将用户信息返回
        SysUserDetails sysUserDetails = (SysUserDetails) authentication.getPrincipal();

        response.getWriter().print(JSON.toJSONString(RespData.success(sysUserDetails.getSysUser())));
        response.getWriter().flush();
    }
}
