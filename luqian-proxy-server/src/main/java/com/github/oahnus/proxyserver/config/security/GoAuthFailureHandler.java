package com.github.oahnus.proxyserver.config.security;

import com.alibaba.fastjson.JSON;
import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.luqiancommon.enums.web.RespCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by oahnus on 2020-04-26
 * 18:44.
 */
public class GoAuthFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        response.getWriter().print(JSON.toJSONString(RespData.error(RespCode.PARAM_ERROR, "登录失败，用户名或密码错误！")));
        response.getWriter().flush();
    }
}
