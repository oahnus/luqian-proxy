package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.config.ProxyTableContainer;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.service.ProxyTableService;
import com.github.oahnus.proxyserver.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by oahnus on 2020-04-16
 * 7:16.
 */
@RestController
@RequestMapping("/proxyTable")
public class ProxyTableController {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private ProxyTableService proxyTableService;

    @PostMapping("/add")
    public RespData addPortMapping(@RequestBody ProxyTable formData) {
        SysUser curUser = sessionService.getCurUser();
        proxyTableService.create(curUser.getId(), formData);
        return RespData.success();
    }

    @GetMapping("/remove")
    public RespData removePortMapping(@RequestParam String appId,
                                  @RequestParam Integer port) {
        SysUser curUser = sessionService.getCurUser();
        proxyTableService.removeProxyTable(curUser.getId(), appId, port);
        return RespData.success();
    }
}
