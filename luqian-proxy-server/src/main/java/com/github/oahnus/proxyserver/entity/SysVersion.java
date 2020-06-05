package com.github.oahnus.proxyserver.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Id;
import java.util.Date;

/**
 * Created by oahnus on 2020-06-03
 * 11:02.
 */
@Data
public class SysVersion {
    @Id
    private Integer id;
    private String version;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date date;
    private String url;
    private String addJson;
    private String uptJson;
    private String delJson;
}
