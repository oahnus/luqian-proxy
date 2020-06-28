package com.github.oahnus.proxyserver.manager;

import com.github.oahnus.proxyserver.entity.SysDomain;
import com.github.oahnus.proxyserver.exceptions.ServiceException;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by oahnus on 2020-06-01
 * 13:50.
 */
public class DomainManager {
    private static Queue<SysDomain> httpDomainPool = new ConcurrentLinkedQueue<>();
    private static Queue<SysDomain> httpsDomainPool = new ConcurrentLinkedQueue<>();

    // 已使用的域名
    private static final Map<Integer, SysDomain> usedDomains = new ConcurrentHashMap<>();

    public static void init(List<SysDomain> domainList) {
        for (SysDomain domain : domainList) {
            if (domain.getHttps()) {
                httpsDomainPool.offer(domain);
            } else {
                httpDomainPool.offer(domain);
            }
        }
    }

    public static void addDomain(SysDomain sysDomain) {
        if (sysDomain == null) {
            return;
        }
        if (sysDomain.getHttps()) {
            httpsDomainPool.offer(sysDomain);
        } else {
            httpDomainPool.offer(sysDomain);
        }
    }

    public static void replaceDomain(SysDomain sysDomain) {
        Integer domainId = sysDomain.getId();
        removeDomain(domainId);
        // 移除后, 将新队列加入队列
        if (sysDomain.getHttps()) {
            httpsDomainPool.add(sysDomain);
        } else {
            httpDomainPool.add(sysDomain);
        }
    }

    public static SysDomain borrowDomain(Boolean isHttps) {
        SysDomain sysDomain = isHttps ? httpsDomainPool.poll() : httpDomainPool.poll();
        if (sysDomain == null) {
            return null;
        }
        usedDomains.put(sysDomain.getPort(), sysDomain);
        return sysDomain;
    }

    public static SysDomain getActiveDomain(Integer port) {
        return usedDomains.get(port);
    }

    public static Boolean isActive(Integer port) {
        return usedDomains.containsKey(port);
    }

    public static void returnDomain(Integer port) {
        SysDomain sysDomain = usedDomains.remove(port);
        if (sysDomain != null) {
            if (sysDomain.getHttps()) {
                httpsDomainPool.offer(sysDomain);
            } else {
                httpDomainPool.offer(sysDomain);
            }
        }
    }

    public static int availableSize() {
        return httpDomainPool.size() + httpsDomainPool.size();
    }

    public static int availableHttpsSize() {
        return httpsDomainPool.size();
    }

    public static void removeDomain(Integer domainId) {
        boolean res1 = httpDomainPool.removeIf(d -> d.getId().equals(domainId));
        boolean res2 = httpsDomainPool.removeIf(d -> d.getId().equals(domainId));
        if (!res1 && !res2) {
            // 两个队列中没有找到对象, 域名可能已被借用
            throw new ServiceException("域名正在使用中, 请稍后再操作");
        }
    }
}
