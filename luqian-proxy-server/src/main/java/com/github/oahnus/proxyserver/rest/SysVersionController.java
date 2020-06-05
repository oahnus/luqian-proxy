package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.PageableParams;
import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.entity.SysVersion;
import com.github.oahnus.proxyserver.service.SysVersionService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by oahnus on 2020-06-03
 * 11:51.
 */
@RestController
@RequestMapping("/version")
public class SysVersionController {
    @Autowired
    private SysVersionService versionService;

    @GetMapping("/page")
    public RespData page(PageableParams params) {
        PageInfo<SysVersion> page = versionService.page(params);
        return RespData.success(page);
    }

    @GetMapping("/versionNumList")
    public RespData versionList() {
        List<String> versionNumList = versionService.getVersionNumList();
        return RespData.success(versionNumList);
    }
}
