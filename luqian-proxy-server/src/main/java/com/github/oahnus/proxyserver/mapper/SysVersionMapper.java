package com.github.oahnus.proxyserver.mapper;

import com.github.oahnus.luqiancommon.mybatis.MyMapper;
import com.github.oahnus.proxyserver.entity.SysVersion;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by oahnus on 2020-06-03
 * 11:04.
 */
public interface SysVersionMapper extends MyMapper<SysVersion> {
    @Select("SELECT version FROM sys_version ORDER BY date DESC")
    List<String> versionNumList();
}
