package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.entity.AppTable;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.service.AppTableService;
import com.github.oahnus.proxyserver.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by oahnus on 2020-04-28
 * 18:47.
 */
@RestController
@RequestMapping("/appTable")
public class AppTableController {
    @Autowired
    private AppTableService appTableService;
    @Autowired
    private SessionService sessionService;

    @GetMapping("/create")
    public RespData genAppIDAndSecret() {
        SysUser curUser = sessionService.getCurUser();
        AppTable appTable = appTableService.createAppTable(curUser.getId());
        return RespData.success(appTable);
    }
    @GetMapping("/list")
    public RespData listAll() {
        SysUser curUser = sessionService.getCurUser();
        List<AppTable> appTableList = appTableService.listByUserId(curUser.getId());
        return RespData.success(appTableList);
    }
}
