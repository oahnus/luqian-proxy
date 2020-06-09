package com.github.oahnus.proxyserver.manager;

import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.github.oahnus.proxyserver.entity.SysAccount;
import com.github.oahnus.proxyserver.enums.SyncStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oahnus on 2020-04-14
 * 7:09.
 */
public class TrafficMeasureMonitor {
    // key -> ${port}
    private static Map<Integer, StatMeasure> measureMap = new ConcurrentHashMap<>();
    // key ${userId}
    private static Map<Long, SysAccount> accountMap = new ConcurrentHashMap<>();
    // 等待同步表
    private static List<StatMeasure> syncWaitList = new CopyOnWriteArrayList<>();
    // 等待表  key -> ${appId}-${port}
    private static Map<String, StatMeasure> waitingMap = new ConcurrentHashMap<>();

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

        StatMeasure statMeasure;
        String key = appId + "-" + port;
        // 检查等待表中是否有当前配置的统计对象
        if (waitingMap.containsKey(key)) {
            statMeasure = waitingMap.get(key);
        } else {
            statMeasure = new StatMeasure(sysUserId, appId, port);
        }

        StatMeasure oldMeasure = measureMap.put(port, statMeasure);
        // 如果端口已有数据,  取出旧统计数据, 加入等待表
        if (oldMeasure != null) {
            key = oldMeasure.getAppId() + "-" + oldMeasure.getPort();
            waitingMap.put(key, oldMeasure);
        }

        // 如果有统计数据未同步到数据库, 加入等待同步列表
        if (oldMeasure != null && SyncStatus.CHANGED.ordinal() == oldMeasure.getSyncStatus().get()) {
            syncWaitList.add(oldMeasure);
        }
    }

    public static List<StatMeasure> flushSyncWaitList() {
        List<StatMeasure> measureList = new ArrayList<>(syncWaitList);
        syncWaitList.removeAll(measureList);
        return measureList;
    }

    public static void removeMeasure(Integer port) {
        StatMeasure measure = measureMap.remove(port);
        if (measure == null) {
            return;
        }
        String key = measure.getAppId() + "-" + measure.getPort();
        waitingMap.put(key, measure);
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
