package com.github.oahnus.proxyserver.entity;

import lombok.Data;

import javax.persistence.Id;

/**
 * Created by oahnus on 2020-06-01
 *
 */
@Data
public class SysDomain {

    @Id
    private Integer id;
    private String name;
    private String domain;
    private Boolean https = false;
    private Integer port;
    private Boolean enable;
}
