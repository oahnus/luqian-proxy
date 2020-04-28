package com.github.oahnus.proxyserver.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Id;
import java.math.BigDecimal;

/**
 * Created by oahnus on 2020-04-22
 * 8:00.
 */
@Data
public class SysUser {
    @Id
    private Long id;

    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JSONField(serialize = false)
    private String password;
    private String email;

    private BigDecimal balance;
}
