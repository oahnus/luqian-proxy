package com.github.oahnus.proxyserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by oahnus on 2020-05-11
 * 18:06.
 */
@Data
public class Statistics {
//    private List<StatItem> portStats = new ArrayList<>();
//    private List<StatItem> appStats = new ArrayList<>();

    private List<StatItem> dateStats = new ArrayList<>();
    @Transient
    private StatUnit statUnit;

    public static @Data class StatItem {
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private Date date;
        private String name;
        private long inBytes;
        private long outBytes;
    }

    public static enum StatUnit {
        B,
        KB,
        MB,
        GB
    }
}
