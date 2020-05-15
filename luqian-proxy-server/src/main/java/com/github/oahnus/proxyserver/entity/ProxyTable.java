package com.github.oahnus.proxyserver.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.Date;

/**
 * Created by oahnus on 2020-04-27
 * 17:37.
 */
@Data
public class ProxyTable {
    @Id
    private String id;
    private String name; // 代理名称
    @NotEmpty
    private String appId;
    private Long sysUserId;
    @NotEmpty
    @Pattern(regexp = "^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})$", message = "服务地址必须为'xxx.xxx.xxx.xxx:xx'形式")
    private String serviceAddr;
    private Integer port;  // server端转发端口
    private Boolean isRandom = false; // 是否使用随机端口
    private Boolean enable;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
