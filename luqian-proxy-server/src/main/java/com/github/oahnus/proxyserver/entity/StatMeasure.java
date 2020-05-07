package com.github.oahnus.proxyserver.entity;

import lombok.Data;

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
    private Long id;
    private String appId;
    private Long userId;
    private int port;
    @Transient
    private AtomicInteger connectCount = new AtomicInteger();
    private transient Long outTrafficBytes = 0L;
    private transient Long inTrafficBytes = 0L;
    private Date date;
//    private Long trafficLimit = 700 * 1024 * 1024L; 700M
    private Long remainTraffic = 5 * 1024L; // 5K

    public StatMeasure() { }

    public StatMeasure(Long sysUserId, String appId, int port) {
        this.userId = sysUserId;
        this.appId = appId;
        this.port = port;
        this.date = new Date();
    }

    public synchronized boolean addOutTrafficBytes(long bytesLen) {
//        if((this.outTrafficBytes + this.inTrafficBytes) > trafficLimit) {
//            return false;
//        }
        this.outTrafficBytes += bytesLen;
        return true;
    }

    public synchronized boolean addInTrafficBytes(long bytesLen) {
        // 检查流量限制
        if((this.outTrafficBytes + this.inTrafficBytes) > remainTraffic) {
            return false;
        }
        this.inTrafficBytes += bytesLen;
        return true;
    }

    public int incrConnectCount() {
        return this.connectCount.incrementAndGet();
    }

    public int decrConnectCount() {
        return this.connectCount.decrementAndGet();
    }
}
