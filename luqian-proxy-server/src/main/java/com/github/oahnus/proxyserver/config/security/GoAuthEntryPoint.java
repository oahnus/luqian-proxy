package com.github.oahnus.proxyserver.config.security;

import com.alibaba.fastjson.JSON;
import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.luqiancommon.enums.web.RespCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by oahnus on 2020-04-26
 * 18:36.
 */
public class GoAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
        response.setHeader("Content-Type", "application/json;charset=utf-8");
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().print(JSON.toJSONString(RespData.error(RespCode.NO_AUTH, "无权限")));
        response.getWriter().flush();
    }
}
