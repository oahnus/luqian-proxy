package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.luqiancommon.util.CaptchaUtils;
import com.github.oahnus.proxyserver.service.CaptchaService;
import com.github.oahnus.proxyserver.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by oahnus on 2020-06-30
 */
@RestController
@RequestMapping("/open")
public class OpenController {
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/captcha/ticket")
    public RespData getCaptchaTicket() throws IOException {
        String ticket = captchaService.ticket();
        return RespData.success(ticket);
    }

    @GetMapping("/captcha")
    public void getCaptchaImg(@RequestParam String ticket, HttpServletResponse response) throws IOException {
        String captcha = captchaService.generate(ticket);
        response.setHeader("Content-Type", "image/jpg");
        CaptchaUtils.captcha(captcha, response.getOutputStream(), false);
    }

    @GetMapping("/captcha/refresh")
    public void refreshCaptcha(@RequestParam String ticket, HttpServletResponse response) throws IOException {
        String captcha = captchaService.generate(ticket);
        response.setHeader("Content-Type", "image/jpg");
        CaptchaUtils.captcha(captcha, response.getOutputStream(), true);
    }

    @GetMapping("/username/exist")
    public RespData checkUsernameIsExist(@RequestParam String username) throws IOException {
        Boolean isExist = sysUserService.checkUsernameIsExist(username);
        return RespData.success(isExist);
    }
}
