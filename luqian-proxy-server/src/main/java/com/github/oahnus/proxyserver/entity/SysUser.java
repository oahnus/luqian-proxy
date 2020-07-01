package com.github.oahnus.proxyserver.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by oahnus on 2020-04-22
 * 8:00.
 */
@Data
public class SysUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotEmpty(message = "用户名不能为空")
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JSONField(serialize = false)
    @NotEmpty(message = "密码不能为空")
    private String password;
    private String email;

    @JSONField(deserialize = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal balance = BigDecimal.ZERO;

    @Transient
    private List<SysPermission> permissionList;
}
