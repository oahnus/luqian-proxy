package com.github.oahnus.proxyserver.entity;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by oahnus on 2020-05-18
 * 17:09.
 */
@Data
@Table(name = "sys_perm")
public class SysPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String value;
    private String url;
    private Boolean enable;
}
