package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.proxyserver.entity.SignInHistory;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.mapper.SignInHistoryMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;

/**
 * Created by oahnus on 2020-05-19
 * 21:45.
 */
@Service
public class SignInHistoryService extends BaseService<SignInHistoryMapper, SignInHistory, Long> {
    private Random rewardGenerator = new Random();

    public SignInHistory createHistory(Long userId, Date date) {
        SignInHistory history = new SignInHistory();
        history.setDate(date);
        history.setCreateTime(new Date());
        history.setUserId(userId);

        int rewards = rewardGenerator.nextInt(9) + 1;
        // 1 - 10 MB流量
        history.setRewards(rewards * 1024 * 1024);
        save(history);

        return history;
    }
}
