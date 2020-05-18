package com.github.oahnus.proxyserver.manager;

import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.StatMeasure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oahnus on 2020-04-14
 * 7:09.
 */
public class TrafficMeasureMonitor {
    // key -> port
    private static Map<Integer, StatMeasure> measureMap = new ConcurrentHashMap<>();

    public static Iterator<Map.Entry<Integer, StatMeasure>> getMeasureIterator() {
        return measureMap.entrySet().iterator();
    }

    public static void init(List<StatMeasure> statMeasures) {
        synchronized (TrafficMeasureMonitor.class) {
            for (StatMeasure statMeasure : statMeasures) {
                Integer port = statMeasure.getPort();
                measureMap.put(port, statMeasure);
            }
        }
    }

    public static StatMeasure getStatMeasure(Integer port) {
        synchronized (port.toString().intern()) {
            return measureMap.get(port);
        }
    }

    public static void createStatMeasure(ProxyTable proxyTable) {
        Integer port = proxyTable.getPort();
        Long sysUserId = proxyTable.getSysUserId();
        String appId = proxyTable.getAppId();

        StatMeasure statMeasure = getStatMeasure(port);
        if (statMeasure == null) {
            statMeasure = new StatMeasure(sysUserId, appId, port);
            measureMap.put(port, statMeasure);
        }
    }

    public static StatMeasure removeMeasure(Integer port) {
        return measureMap.remove(port);
    }

    public static String printStatInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=========Monitor=========\n");
        for (Map.Entry<Integer, StatMeasure> entry : measureMap.entrySet()) {
            Integer port = entry.getKey();
            StatMeasure measure = entry.getValue();

            String msg = String.format("APP_ID: %s, Port: %s, ConnectCount: %s, InBytes: %s, OutBytes: %s\n",
                    measure.getAppId(), port, measure.getConnectCount(), measure.getInTrafficBytes(), measure.getOutTrafficBytes());
            sb.append(msg);
        }
        sb.append("=========================\n");
        return sb.toString();
    }

    public static List<StatMeasure> reset() {
        List<StatMeasure> oldMeasureList = new ArrayList<>();
        for (StatMeasure measure : measureMap.values()){
            StatMeasure newVal = new StatMeasure(measure.getUserId(), measure.getAppId(), measure.getPort());
            measureMap.replace(measure.getPort(), newVal);
            oldMeasureList.add(measure);
        }
        return oldMeasureList;
    }
}
