package com.github.oahnus.proxyserver.manager;

import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.github.oahnus.proxyserver.entity.SysAccount;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oahnus on 2020-04-14
 * 7:09.
 */
public class TrafficMeasureMonitor {
    // key -> port
    private static Map<Integer, StatMeasure> measureMap = new ConcurrentHashMap<>();
    // key userId
    private static Map<Long, SysAccount> accountMap = new ConcurrentHashMap<>();

    public static Iterator<Map.Entry<Integer, StatMeasure>> getMeasureIterator() {
        return measureMap.entrySet().iterator();
    }
    public static Iterator<Map.Entry<Long, SysAccount>> getAccountIterator() {
        return accountMap.entrySet().iterator();
    }

    public static void init(List<StatMeasure> statMeasures, List<SysAccount> accountList) {
        for (StatMeasure statMeasure : statMeasures) {
            Integer port = statMeasure.getPort();
            measureMap.put(port, statMeasure);
        }
        for (SysAccount account : accountList) {
            Long userId = account.getUserId();
            accountMap.put(userId, account);
        }
    }

    public static SysAccount getAccount(Long userId) {
        return accountMap.get(userId);
    }

    public static void createStatMeasure(ProxyTable proxyTable) {
        Integer port = proxyTable.getPort();
        Long sysUserId = proxyTable.getSysUserId();
        String appId = proxyTable.getAppId();

        if (!measureMap.containsKey(port)) {
            measureMap.put(port, new StatMeasure(sysUserId, appId, port));
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

    public static boolean addInTrafficBytes(int port, int byteLen) {
        StatMeasure measure = measureMap.get(port);

        Long userId = measure.getUserId();
        SysAccount sysAccount = accountMap.get(userId);

        // 检查已用流量是否超过流量限额
        if (sysAccount.getUsedTraffic() > sysAccount.getTrafficLimit()) {
            return false;
        }
        // 记录
        measure.addInTrafficBytes(byteLen);
        sysAccount.addUsedTraffic(byteLen);
        return true;
    }

    public static void addOutTrafficBytes(int port, int byteLen) {
        StatMeasure measure = measureMap.get(port);
        measure.addOutTrafficBytes(byteLen);

        Long userId = measure.getUserId();
        SysAccount sysAccount = accountMap.get(userId);
        sysAccount.addUsedTraffic(byteLen);
    }

    public static void decrConnectCount(int port) {
        StatMeasure measure = measureMap.get(port);
        measure.getConnectCount().decrementAndGet();
    }
    public static void incrConnectCount(int port) {
        StatMeasure measure = measureMap.get(port);
        measure.getConnectCount().incrementAndGet();
    }

    public static void clearInactivePorts(Collection<Integer> activePorts) {
        measureMap.keySet().forEach(port -> {
            if (!activePorts.contains(port)) {
                removeMeasure(port);
            }
        });
    }
}
