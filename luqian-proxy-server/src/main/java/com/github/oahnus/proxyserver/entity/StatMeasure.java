package com.github.oahnus.proxyserver.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.oahnus.proxyserver.enums.SyncStatus;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oahnus on 2020-04-14
 * 7:04.
 */
@Data
public class StatMeasure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String appId;
    private Long userId;
    private Integer port;
    @Transient
    private AtomicInteger connectCount = new AtomicInteger();
    private volatile Long outTrafficBytes = 0L;
    private volatile Long inTrafficBytes = 0L;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date date;
//    private Long trafficLimit = 700 * 1024 * 1024L; 700M
    private Long remainTraffic = 5 * 1024L; // 5K
    @Transient
    private AtomicInteger syncStatus = new AtomicInteger();

    public StatMeasure() { }

    public StatMeasure(Long sysUserId, String appId, int port) {
        this.userId = sysUserId;
        this.appId = appId;
        this.port = port;
        this.date = new Date();
    }

    public synchronized long addOutTrafficBytes(long bytesLen) {
        this.outTrafficBytes += bytesLen;
        syncStatus.set(SyncStatus.CHANGED.ordinal());
        AtomicInteger syncVersion = new AtomicInteger();
        // 0 未更新 1 正在更新 2 待更新
        return this.outTrafficBytes;
    }

    public synchronized long addInTrafficBytes(long bytesLen) {
        this.inTrafficBytes += bytesLen;
        syncStatus.set(SyncStatus.CHANGED.ordinal());
        return this.inTrafficBytes;
    }

    public int incrConnectCount() {
        return this.connectCount.incrementAndGet();
    }

    public int decrConnectCount() {
        return this.connectCount.decrementAndGet();
    }
}
