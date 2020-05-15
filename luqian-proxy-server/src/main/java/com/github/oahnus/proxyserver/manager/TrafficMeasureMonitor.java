package com.github.oahnus.proxyserver.manager;

import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.sun.org.apache.bcel.internal.generic.NEW;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oahnus on 2020-04-14
 * 7:09.
 */
public class TrafficMeasureMonitor {
    // key -> port
    private static Map<Integer, StatMeasure> measureMap = new ConcurrentHashMap<>();
    private static Map<Integer, StatMeasure> temp = null;

    // todo refact
    public static Iterator<Map.Entry<Integer, StatMeasure>> getMeasureIterator() {
        return measureMap.entrySet().iterator();
    }

    public static void reset() {
        // 复制数据，生成新的map, 然后归档
        Date date = new Date();
        synchronized (TrafficMeasureMonitor.class) {
            temp = new HashMap<>();
            Iterator<Map.Entry<Integer, StatMeasure>> iterator = getMeasureIterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, StatMeasure> next = iterator.next();
                Integer port = next.getKey();
                StatMeasure statMeasure = next.getValue();

                temp.put(port, statMeasure);
                StatMeasure newMeasure = new StatMeasure();
                newMeasure.setDate(date);
                newMeasure.setAppId(statMeasure.getAppId());
                newMeasure.setPort(port);
//                newMeasure.setRemainTraffic(0);
                measureMap.put(port, newMeasure);
            }
        }
    }

    public static StatMeasure getStatMeasure(Integer port) {
        return measureMap.get(port);
    }

    public static StatMeasure createStatMeasure(ProxyTable proxyTable) {
        Integer port = proxyTable.getPort();
        Long sysUserId = proxyTable.getSysUserId();
        String appId = proxyTable.getAppId();

        StatMeasure statMeasure = getStatMeasure(port);
        if (statMeasure == null) {
            statMeasure = new StatMeasure(sysUserId, appId, port);
            measureMap.put(port, statMeasure);
        }
        return statMeasure;
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
}
