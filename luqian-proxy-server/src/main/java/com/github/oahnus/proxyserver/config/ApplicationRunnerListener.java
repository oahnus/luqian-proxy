package com.github.oahnus.proxyserver.config;

import com.github.oahnus.luqiancommon.util.AESUtils;
import com.github.oahnus.proxyserver.entity.AppTable;
import com.github.oahnus.proxyserver.service.AppTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oahnus on 2020/4/21
 * 6:45.
 */
@Component
public class ApplicationRunnerListener implements ApplicationRunner {
    @Value("${proxy.aes.secret}")
    private String aesSecret;
    @Value("${proxy.aes.offset}")
    private String aesOffset;

    @Autowired
    AppTableService appTableService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        AESUtils.init(aesOffset, aesSecret);

        List<AppTable> appTableList = appTableService.loadAll();
        if (!CollectionUtils.isEmpty(appTableList)) {
            for (AppTable appTable : appTableList) {
                ProxyTableContainer.getInstance().addApplication(appTable.getAppId(), appTable.getAppSecret());
            }
        }
    }
}