package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.util.RandomUtils;
import com.github.oahnus.luqiancommon.util.encrypt.MD5Utils;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Created by oahnus on 2020-06-30
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {
    private static final String PLACEHOLDER = "#";
    private Cache<String, String> captchaMap = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    @Override
    public String ticket() {
        String time = String.valueOf(System.currentTimeMillis());
        String ticket = MD5Utils.generateMD5(time);
        captchaMap.put(ticket, PLACEHOLDER);
        return ticket;
    }

    @Override
    public String generate(String ticket) {
        if (StringUtils.isEmpty(ticket)) {
            throw new ServiceException("非法ticket");
        }
        String placeholder = captchaMap.getIfPresent(ticket);
        if (StringUtils.isEmpty(placeholder)) {
            throw new ServiceException("ticket不存在");
        }
        if (!placeholder.equals(PLACEHOLDER)) {
            throw new ServiceException("ticket已失效");
        }
        String captcha = RandomUtils.genNChars(4, RandomUtils.MODE.ALL, false);
        captchaMap.put(ticket, captcha);
        return captcha;
    }

    @Override
    public Boolean validate(String ticket, String captcha) {
        if (StringUtils.isEmpty(ticket) || StringUtils.isEmpty(captcha)) {
            return false;
        }
        String cachedCaptcha = captchaMap.getIfPresent(ticket);
        captchaMap.invalidate(ticket);
        return cachedCaptcha != null && cachedCaptcha.toUpperCase().equals(captcha.toUpperCase());
    }
}
