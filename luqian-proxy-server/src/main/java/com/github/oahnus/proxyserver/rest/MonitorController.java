package com.github.oahnus.proxyserver.rest;

import com.github.oahnus.luqiancommon.dto.RespData;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by oahnus on 2020-04-21
 * 6:50.
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {
    @GetMapping("print")
    public RespData print() {
        String info = TrafficMeasureMonitor.printStatInfo();
        return RespData.success(info);
    }
}
