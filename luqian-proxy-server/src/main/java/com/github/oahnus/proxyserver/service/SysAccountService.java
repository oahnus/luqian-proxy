package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.util.DateUtils;
import com.github.oahnus.proxyserver.entity.SignInHistory;
import com.github.oahnus.proxyserver.entity.SysAccount;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import com.github.oahnus.proxyserver.mapper.SysAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Created by oahnus on 2020-05-19
 * 12:02.
 */
@Service
@Slf4j
public class SysAccountService extends BaseService<SysAccountMapper, SysAccount, Long> {

    @Autowired
    private SignInHistoryService signInHistoryService;

    public void createAccount(SysUser sysUser) {
        Long userId = sysUser.getId();
        SysAccount account = new SysAccount();
        account.setUsedTraffic(0L);
        account.setUserId(userId);
        account.setTrafficLimit(700*1024*1024L);

        save(account);
    }

    @Scheduled(cron = "0 */2 * * * *")
    public void sync() {
        log.info("Sync Account");
        Iterator<Map.Entry<Long, SysAccount>> iterator = TrafficMeasureMonitor.getAccountIterator();
        List<SysAccount> accountList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<Long, SysAccount> entry = iterator.next();
            accountList.add(entry.getValue());
        }
        updateBatchById(accountList);
        log.info("Sync Account Finish");
    }

    public SysAccount getAccountByUserId(Long userId) {
        return selectOne(new QueryBuilder(SysAccount.class).eq("userId", userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public long signIn(SysUser curUser) {
        try {
            Long userId = curUser.getId();
            Date today = DateUtils.localDate2date(LocalDate.now());

            SignInHistory existedHistory = signInHistoryService.selectOne(new QueryBuilder(SignInHistory.class)
                    .eq("date", today)
                    .eq("userId", userId));
            if (existedHistory != null) {
                throw new ServiceException("本日已签到过, 请勿重复签到");
            }

            SignInHistory history = signInHistoryService.createHistory(userId, today);
            Integer rewards = history.getRewards();

            SysAccount monitorAcct = TrafficMeasureMonitor.getAccount(userId);
            Long trafficLimit = monitorAcct.getTrafficLimit();
            trafficLimit = trafficLimit + rewards;

            monitorAcct.setTrafficLimit(trafficLimit);

            SysAccount account = getAccountByUserId(userId);
            account.setTrafficLimit(trafficLimit);
            updateById(account);
            return rewards;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            StackTraceElement element = e.getStackTrace()[0];
            log.error("File: {}, Line: {}, Method: {}, Message: {}",
                    element.getFileName(),
                    element.getLineNumber(),
                    element.getMethodName(), e.getMessage());
            throw new ServiceException("签到失败, 请稍后重试");
        }
    }
}
