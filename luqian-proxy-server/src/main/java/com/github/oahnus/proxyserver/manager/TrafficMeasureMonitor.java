package com.github.oahnus.proxyserver.manager;

import com.github.oahnus.proxyserver.entity.StatMeasure;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oahnus on 2020-04-14
 * 7:09.
 */
public class TrafficMeasureMonitor {
    // key -> port
    private static Map<Integer, StatMeasure> measureMap = new ConcurrentHashMap<>();

    public static StatMeasure getStatMeasure(Integer port) {
        return measureMap.get(port);
    }

    public static StatMeasure createStatMeasure(String appId, Integer port) {
        StatMeasure statMeasure = getStatMeasure(port);
        if (statMeasure == null) {
            statMeasure = new StatMeasure(appId, port, new Date());
            measureMap.put(port, statMeasure);
        }
        return statMeasure;
    }

    public static StatMeasure removeMeasure(Integer port) {
        return measureMap.remove(port);
    }

    public static void printStatInfo() {
        System.out.println("=========Monitor=========");
        for (Map.Entry<Integer, StatMeasure> entry : measureMap.entrySet()) {
            Integer port = entry.getKey();
            StatMeasure measure = entry.getValue();

            String msg = String.format("APP_ID: %s, Port: %s, ConnectCount: %s, InBytes: %s, OutBytes: %s",
                    measure.getAppId(), port, measure.getConnectCount(), measure.getInTrafficBytes(), measure.getOutTrafficBytes());
            System.out.println(msg);
        }
        System.out.println("=========================");
    }
}
