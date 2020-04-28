package com.github.oahnus.proxyserver.config.security;

import com.alibaba.fastjson.JSON;
import com.github.oahnus.luqiancommon.dto.RespData;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by oahnus on 2020-04-26
 * 18:33.
 */
public class GoLogoutSuccessHandler implements LogoutSuccessHandler {
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.setHeader("Content-Type", "application/json:charset=utf-8");
        response.getWriter().print(JSON.toJSONString(RespData.success()));
    }
}
