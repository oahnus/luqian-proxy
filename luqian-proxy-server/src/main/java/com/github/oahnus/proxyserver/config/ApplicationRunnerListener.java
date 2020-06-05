package com.github.oahnus.proxyserver.config;

import com.github.oahnus.luqiancommon.biz.QueryBuilder;
import com.github.oahnus.luqiancommon.util.DateUtils;
import com.github.oahnus.luqiancommon.util.encrypt.AESUtils;
import com.github.oahnus.proxyserver.bootstrap.ProxyServer;
import com.github.oahnus.proxyserver.entity.AppTable;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.github.oahnus.proxyserver.entity.SysAccount;
import com.github.oahnus.proxyserver.entity.SysDomain;
import com.github.oahnus.proxyserver.manager.DomainManager;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import com.github.oahnus.proxyserver.service.AppTableService;
import com.github.oahnus.proxyserver.service.StatMeasureService;
import com.github.oahnus.proxyserver.service.SysAccountService;
import com.github.oahnus.proxyserver.service.SysDomainService;
import com.github.oahnus.proxyserver.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Created by oahnus on 2020/4/21
 * 6:45.
 */
@Slf4j
@Component
public class ApplicationRunnerListener implements ApplicationRunner {
    @Value("${proxy.aes.secret}")
    private String aesSecret;
    @Value("${proxy.aes.offset}")
    private String aesOffset;
    @Value("${proxy.jwt.secret}")
    private String jwtSecret;

    @Autowired
    private AppTableService appTableService;
    @Autowired
    private StatMeasureService measureService;
    @Autowired
    private SysAccountService accountService;
    @Autowired
    private SysDomainService sysDomainService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        AESUtils.init(aesOffset, aesSecret);
        JwtUtils.init(jwtSecret);

        // 加载应用数据
        List<AppTable> appTableList = appTableService.loadAll();
        log.info("Load App Table Size: {}.", appTableList.size());
        if (!CollectionUtils.isEmpty(appTableList)) {
            for (AppTable appTable : appTableList) {
                ProxyTableContainer.getInstance().addApplication(appTable.getAppId(), appTable.getAppSecret());
            }
        }

        // 加载流量统计数据
        log.info("Load StatMeasure.");
        Date today = DateUtils.localDate2date(LocalDate.now());
        List<StatMeasure> measureList = measureService.selectList(new QueryBuilder(StatMeasure.class)
                .eq("date", today));

        // 加载账户数据
        List<SysAccount> accountList = accountService.selectAll();
        log.info("Load Sys Account Size: {}.", accountList.size());
        TrafficMeasureMonitor.init(measureList, accountList);

        // 加载域名池
        List<SysDomain> sysDomains = sysDomainService.availableList();
        log.info("Load Domain Size: {}.", sysDomains.size());
        DomainManager.init(sysDomains);

        // 启动netty服务
        log.info("Ready Start Proxy Server.");
        ProxyServer.getInstance().start();
    }
}
