package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.proxyserver.entity.SysPermission;
import com.github.oahnus.proxyserver.entity.SysUserPerm;
import com.github.oahnus.proxyserver.mapper.SysPermMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by oahnus on 2020-05-18
 * 17:20.
 */
@Service
public class SysPermService extends BaseService<SysPermMapper, SysPermission, Long> {
    @Autowired
    SysUserPermService userPermService;

    public List<SysPermission> listByUserId(Long userId) {
        List<SysUserPerm> permRelations = userPermService.selectList(new QueryBuilder(SysUserPerm.class)
                .eq("userId", userId));

        if (CollectionUtils.isEmpty(permRelations)) {
            return Collections.emptyList();
        }
        List<Long> permIds = permRelations.stream()
                .map(SysUserPerm::getPermId)
                .collect(Collectors.toList());

        return selectList(new QueryBuilder(SysPermission.class)
                .in("id", permIds)
                .eq("enable", true));
    }
}
