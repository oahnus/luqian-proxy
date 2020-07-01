package com.github.oahnus.proxyserver.service;

/**
 * Created by oahnus on 2020-06-30
 */
public interface CaptchaService {
    String ticket();
    String generate(String ticket);
    Boolean validate(String ticket, String captcha);
}
