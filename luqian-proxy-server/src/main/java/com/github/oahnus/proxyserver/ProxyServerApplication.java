package com.github.oahnus.proxyserver;

import com.github.oahnus.proxyserver.config.ProxyTable;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;


/**
 * Created by oahnus on 2020-03-31
 * 14:34.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
public class ProxyServerApplication {
    private int syncVersion;

    public static void main(String[] args) {
        SpringApplication.run(ProxyServerApplication.class, args);
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void printMonitor() {
        TrafficMeasureMonitor.printStatInfo();
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 60000)
    public void syncConfig() throws IOException {
        int version = ProxyTable.getVersion();
        if (version != syncVersion) {
            ProxyTable.saveToDisk();
            syncVersion = version;
        }
    }
}
