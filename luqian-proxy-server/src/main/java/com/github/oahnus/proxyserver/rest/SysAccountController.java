package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.service.SessionService;
import com.github.oahnus.proxyserver.service.SysAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by oahnus on 2020-05-19
 * 21:35.
 */
@RestController
@RequestMapping("/acct")
public class SysAccountController {
    @Autowired
    private SysAccountService accountService;
    @Autowired
    private SessionService sessionService;

    @GetMapping("/signIn")
    public RespData signIn() {
        SysUser curUser = sessionService.getCurUser();
        long rewards = accountService.signIn(curUser);
        return RespData.success(rewards);
    }
}
