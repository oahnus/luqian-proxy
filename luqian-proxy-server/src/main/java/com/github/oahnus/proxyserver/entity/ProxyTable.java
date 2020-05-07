package com.github.oahnus.proxyserver.entity;

import lombok.Data;

import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;

/**
 * Created by oahnus on 2020-04-27
 * 17:37.
 */
@Data
public class ProxyTable {
    @Id
    private Long id;
    @NotEmpty
    private String appId;
    @NotNull
    private Long sysUserId;
    @NotEmpty
    @Pattern(regexp = "^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})$", message = "服务地址必须为'xxx.xxx.xxx.xxx:xx'形式")
    private String serviceAddr;
    private Integer port;  // server端转发端口, null时随机
    private Boolean enable;

    private Date createTime;
}
