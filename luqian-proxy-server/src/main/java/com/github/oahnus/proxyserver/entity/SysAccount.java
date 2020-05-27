package com.github.oahnus.proxyserver.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    public void addUsedTraffic(long deltaBytes) {
        synchronized (this) {
            this.usedTraffic += deltaBytes;
        }
    }
}
