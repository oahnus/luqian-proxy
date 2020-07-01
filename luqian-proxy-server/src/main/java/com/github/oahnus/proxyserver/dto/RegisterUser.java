package com.github.oahnus.proxyserver.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

/**
 * Created by oahnus on 2020-06-30
 */
@Data
public class RegisterUser {
    @NotEmpty(message = "用户名不能为空")
    private String username;
    @NotEmpty(message = "密码不能为空")
    private String password;
    private String email;
    @NotEmpty(message = "验证码不能为空")
    private String captcha;
    @NotEmpty(message = "不能为空")
    private String ticket;
}
