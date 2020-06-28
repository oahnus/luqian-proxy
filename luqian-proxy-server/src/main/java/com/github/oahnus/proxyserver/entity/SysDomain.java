package com.github.oahnus.proxyserver.entity;

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.*;

/**
 * Created by oahnus on 2020-06-01
 *
 */
@Data
public class SysDomain {
    @Id
    private Integer id;
    @NotEmpty(message = "name不能为空")
    private String name;
    @NotEmpty(message = "域名不能为空")
    @Pattern(regexp = "([0-9a-zA-Z]+.)+[0-9a-zA-Z]+", message = "域名格式错误")
    private String domain;
    private Boolean https = false;
    @NotNull(message = "域名监听端口不能为空")
    @Min(value = 30001, message = "域名端口不能小于30001")
    @Max(value = 31000, message = "域名端口不能大于31000")
    private Integer port;
    private Boolean enable;

    @Transient
    private Boolean isActive;
}
