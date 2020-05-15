package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.dto.Statistics;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import com.github.oahnus.proxyserver.service.SessionService;
import com.github.oahnus.proxyserver.service.StatMeasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by oahnus on 2020-04-21
 * 6:50.
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {
    @Autowired
    StatMeasureService statMeasureService;
    @Autowired
    SessionService sessionService;

    @GetMapping("print")
    public RespData print() {
        String info = TrafficMeasureMonitor.printStatInfo();
        return RespData.success(info);
    }

    @GetMapping("/stat")
    public RespData listStatInfo(StatMeasure params) {
        SysUser curUser = sessionService.getCurUser();
        params.setUserId(curUser.getId());
//        List<StatMeasure> measureList = statMeasureService.queryList(params);
//        return RespData.success(measureList);
        Statistics statistics = statMeasureService.genStatReport(curUser.getId());
        return RespData.success(statistics);
    }
}
