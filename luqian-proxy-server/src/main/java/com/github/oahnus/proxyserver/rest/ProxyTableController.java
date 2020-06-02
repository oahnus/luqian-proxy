package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.manager.DomainManager;
import com.github.oahnus.proxyserver.service.ProxyTableService;
import com.github.oahnus.proxyserver.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public RespData addPortMapping(@RequestBody @Valid ProxyTable formData) {
        SysUser curUser = sessionService.getCurUser();
        formData.setSysUserId(curUser.getId());
        proxyTableService.create(formData);
        return RespData.success();
    }

    @PostMapping("/update")
    public RespData update(@RequestBody @Valid ProxyTable proxyTable) {
        SysUser curUser = sessionService.getCurUser();
        proxyTable.setSysUserId(curUser.getId());
        proxyTableService.updateInfo(proxyTable);

        return RespData.success();
    }

    @GetMapping("/remove")
    public RespData removePortMapping(@RequestParam String id) {
        SysUser curUser = sessionService.getCurUser();
        proxyTableService.removeProxyTable(id, curUser.getId());
        return RespData.success();
    }

    @GetMapping("/list")
    public RespData listAll(@RequestParam(required = false) String appId) {
        SysUser curUser = sessionService.getCurUser();
        List<ProxyTable> proxyTableList = proxyTableService.findList(appId, curUser.getId());
        return RespData.success(proxyTableList);
    }

    @GetMapping("/domain/size")
    public RespData checkAvailableDomainSize() {
        int availableSize = DomainManager.availableSize();
        int httpsSize = DomainManager.availableHttpsSize();
        Map<String, Integer> dataMap = new HashMap<>(4);
        dataMap.put("availableSize", availableSize);
        dataMap.put("httpsSize", httpsSize);
        return RespData.success(dataMap);
    }
}
