package com.github.oahnus.proxyserver.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by oahnus on 2020-04-14
 * 7:48.
 */
@Data
public class ProxyTableItem implements Serializable {
    private int port;
    private String serviceAddr;
    private String appId;
}
