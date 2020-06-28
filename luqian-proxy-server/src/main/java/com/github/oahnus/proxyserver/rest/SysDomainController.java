package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.entity.SysDomain;
import com.github.oahnus.proxyserver.service.SysDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Created by oahnus on 2020-06-09
 */
@RestController
@RequestMapping("/domain")
public class SysDomainController {

    @Autowired
    private SysDomainService domainService;

    @PreAuthorize("hasAuthority('queryDomain')")
    @GetMapping("/list")
    public RespData get() {
        List<SysDomain> domainList = domainService.domainList();
        return RespData.success(domainList);
    }

    @PreAuthorize("hasAuthority('addDomain')")
    @PostMapping("create")
    public RespData addDomain(@RequestBody @Valid SysDomain sysDomain) {
        String configStr = domainService.addNewDomain(sysDomain);
        return RespData.success(configStr);
    }

    @PostMapping("/status")
    public RespData changeEnableStatus(@RequestParam Integer id,
                                       @RequestParam Boolean enable) {
        domainService.changeEnableStatus(id, enable);
        return RespData.success();
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('updateDomain')")
    public RespData updateDomain(@RequestBody @Valid SysDomain sysDomain) {
        domainService.update(sysDomain);
        return RespData.success();
    }
}
