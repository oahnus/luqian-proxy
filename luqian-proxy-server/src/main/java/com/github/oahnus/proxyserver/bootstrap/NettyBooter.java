package com.github.oahnus.proxyserver.bootstrap;

import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by oahnus on 2020-03-31
 * 14:35.
 */
@Component
public class NettyBooter implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent cre) {
        if (cre.getApplicationContext().getParent() == null) {
            try {
                ProxyServer.getInstance().start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
