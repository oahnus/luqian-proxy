package com.github.oahnus.proxyserver.service;

import com.github.oahnus.luqiancommon.biz.BaseService;
import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.proxyserver.dto.RegisterUser;
import com.github.oahnus.proxyserver.entity.SysUser;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Created by oahnus on 2020-04-23
 * 7:18.
 */
@Service
public class SysUserService extends BaseService<SysUserMapper, SysUser, Long> {
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    SysAccountService accountService;
    @Autowired
    CaptchaService captchaService;

    public SysUser getUserByUsername(String username) {
        return selectOne(new QueryBuilder(SysUser.class)
                .eq("username", username));
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUser(RegisterUser registerUser) {
        String captcha = registerUser.getCaptcha();
        String username = registerUser.getUsername();

        Boolean valid = captchaService.validate(registerUser.getTicket(), captcha);

        if (!valid) {
            throw new ServiceException("验证失败，请刷新重试");
        }

        List<SysUser> userList = selectList(new QueryBuilder(SysUser.class)
                .eq("username", username));

        if (!CollectionUtils.isEmpty(userList)) {
            throw new ServiceException("用户名已存在");
        }

        SysUser sysUser = new SysUser();
        sysUser.setUsername(username);
        sysUser.setEmail(registerUser.getEmail());

        String password = registerUser.getPassword();
        sysUser.setPassword(passwordEncoder.encode(password));

        save(sysUser);

        // 创建流量账户
        accountService.createAccount(sysUser);
    }

    public Boolean checkUsernameIsExist(String username) {
        List<SysUser> list = selectList(new QueryBuilder(SysUser.class)
                .eq("username", username));
        return !CollectionUtils.isEmpty(list);
    }
}
