package com.github.oahnus.proxyserver.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;

/**
 * Created by oahnus on 2020-04-22
 * 8:00.
 */
@Data
public class SysUser {
    @Id
    private Long id;
    @NotEmpty
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JSONField(serialize = false)
    @NotEmpty
    private String password;
    private String email;

    @JSONField(deserialize = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal balance;
}
