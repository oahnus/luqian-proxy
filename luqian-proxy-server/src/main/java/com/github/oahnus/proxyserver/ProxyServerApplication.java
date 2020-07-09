package com.github.oahnus.proxyserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import tk.mybatis.spring.annotation.MapperScan;


/**
 * Created by oahnus on 2020-03-31
 * 14:34.
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.github.oahnus.proxyserver.mapper")
public class ProxyServerApplication {
    private int syncVersion;

    public static void main(String[] args) {
        SpringApplication.run(ProxyServerApplication.class, args);
    }

//    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
//    public void printMonitor() {
//        String info = TrafficMeasureMonitor.printStatInfo();
//        System.out.println(info);
//    }

//    @Scheduled(initialDelay = 10000, fixedDelay = 60000)
//    public void syncConfig() throws IOException {
//        int version = ProxyTableContainer.getVersion();
//        if (version != syncVersion) {
//            ProxyTableContainer.saveToDisk();
//            syncVersion = version;
//        }
//    }
}
