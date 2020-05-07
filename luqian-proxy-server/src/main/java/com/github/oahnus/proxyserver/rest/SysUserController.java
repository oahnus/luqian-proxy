package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.service.SessionService;
import com.github.oahnus.proxyserver.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Created by oahnus on 2020-05-01
 * 17:28.
 */
@RestController
@RequestMapping
public class SysUserController {
    @Autowired
    SysUserService userService;
    @Autowired
    SessionService sessionService;

    @PostMapping("register")
    public RespData register(@RequestBody @Valid SysUser sysUser) {
        userService.createUser(sysUser);
        return RespData.success();
    }

    @GetMapping("/userInfo")
    public RespData userInfo() {
        SysUser curUser = sessionService.getCurUser();
        return RespData.success(curUser);
    }
}
