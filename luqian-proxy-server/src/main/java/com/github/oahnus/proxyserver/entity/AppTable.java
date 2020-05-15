package com.github.oahnus.proxyserver.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import java.util.Date;

/**
 * Created by oahnus on 2020-04-28
 * 12:29.
 */
@Data
public class AppTable {
    @Id
    private Long id;
    private String appId;
    private String name;
    private String appSecret;
    private Long sysUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
