package com.github.oahnus.proxyserver.entity;

import lombok.Data;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oahnus on 2020-04-14
 * 7:04.
 */
@Data
public class StatMeasure {
    private Long id;
    private String appId;
    private int port;
    private AtomicInteger connectCount = new AtomicInteger();
    private transient long outTrafficBytes = 0;
    private transient long inTrafficBytes = 0;
    private Date date;

    public StatMeasure() { }
    public StatMeasure(String appId, int port, Date date) {
        this.appId = appId;
        this.port = port;
        this.date = date;
    }

    public synchronized long addOutTrafficBytes(long bytesLen) {
        this.outTrafficBytes += bytesLen;
        return this.outTrafficBytes;
    }

    public synchronized long addInTrafficBytes(long bytesLen) {
        this.inTrafficBytes += bytesLen;
        return this.inTrafficBytes;
    }

    public int incrConnectCount() {
        return this.connectCount.incrementAndGet();
    }

    public int decrConnectCount() {
        return this.connectCount.decrementAndGet();
    }
}
