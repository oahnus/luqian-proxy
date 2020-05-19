package com.github.oahnus.proxyserver.entity;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Created by oahnus on 2020-05-19
 * 11:39.
 */
@Data
public class SysAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long trafficLimit;
    private Long usedTraffic;

    public void addUsedTraffic(long deltaBytes) {
        synchronized (this) {
            this.usedTraffic += deltaBytes;
        }
    }
}
