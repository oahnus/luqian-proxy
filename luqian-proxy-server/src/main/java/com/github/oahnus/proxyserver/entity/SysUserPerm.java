package com.github.oahnus.proxyserver.entity;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Created by oahnus on 2020-05-18
 * 17:23.
 */
@Data
public class SysUserPerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long permId;
}
