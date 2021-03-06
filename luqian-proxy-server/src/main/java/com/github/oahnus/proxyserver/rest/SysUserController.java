package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.dto.RegisterUser;
import com.github.oahnus.proxyserver.entity.SysPermission;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.service.SessionService;
import com.github.oahnus.proxyserver.service.SysPermService;
import com.github.oahnus.proxyserver.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Created by oahnus on 2020-05-01
 * 17:28.
 */
@RestController
@RequestMapping
public class SysUserController {
    @Autowired
    private SysUserService userService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private SysPermService sysPermService;

    @PostMapping("register")
    public RespData register(@RequestBody @Valid RegisterUser registerUser) {
        userService.createUser(registerUser);
        return RespData.success();
    }

    @GetMapping("/userInfo")
    public RespData userInfo() {
        SysUser curUser = sessionService.getCurUser();
        List<SysPermission> permissionList = sysPermService.listByUserId(curUser.getId());
        curUser.setPermissionList(permissionList);
        return RespData.success(curUser);
    }
}
