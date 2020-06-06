package com.github.oahnus.proxyserver.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.SysDomain;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
import com.github.oahnus.proxyserver.manager.DomainManager;
import com.github.oahnus.proxyserver.service.ProxyTableService;
import com.github.oahnus.proxyserver.utils.RandomPortUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by oahnus on 2020-04-03
 * 15:14.
 */
@Component
public class ProxyTableContainer extends Observable {
    // 代理记录
    private static Map<Integer, ProxyTable> proxyTableMap = new ConcurrentHashMap<>();
    // 已授权用户 key appId secret apSecret
    private static Map<String, String> applicationMap = new ConcurrentHashMap<>(32);

    private static ProxyTableContainer INSTANCE;

    @Autowired
    private ProxyTableService proxyTableService;

    private ProxyTableContainer() {}

    @PostConstruct
    public void init() {
        INSTANCE = this;
    }

    public static void saveToDisk() throws IOException {
        System.out.println("Save Config");
        File file = new File("server.json");
        OutputStream out = new FileOutputStream(file);
        String s1 = JSON.toJSONString(proxyTableMap);
        String s2 = JSON.toJSONString(applicationMap);

        String s = s1 + "#####" + s2;
        out.write(s.getBytes());
        out.flush();
        out.close();
    }

    public static void loadFromDisk() throws IOException {
        System.out.println("Load Config");
        File file = new File("server.json");
        if (!file.exists()) {
            return;
        }
        InputStream in = new FileInputStream(file);
        byte[] bytes = new byte[in.available()];
        IOUtils.readFully(in, bytes);
        String s = new String(bytes);
        if (StringUtils.isEmpty(s)) {
            return;
        }
        String[] strings = s.split("#####");
        if (strings.length != 2) {
            return;
        }
        proxyTableMap = JSON.parseObject(strings[0], new TypeReference<Map<Integer, ProxyTable>>() {});
        applicationMap = JSON.parseObject(strings[1], new TypeReference<Map<String, String>>() {});
        in.close();
    }

    public String getAppSecret(String appId) {
        return applicationMap.get(appId);
    }

    public synchronized String addApplication(String appId, String appSecret) {
        return applicationMap.put(appId, appSecret);
    }

    public Map<Integer, ProxyTable> proxyTableMap() {
        return proxyTableMap;
//        if (proxyTableMap.isEmpty()) {
//            // 如果map为空，尝试从数据库读取proxy配置
//            List<ProxyTable> tableList = proxyTableService.loadAllActive();
//            if (!CollectionUtils.isEmpty(tableList)) {
//                // 将固定port的配置表先存入map, 避免与随机端口冲突
//                tableList.stream()
//                        .filter(pt -> !pt.getIsRandom())
//                        .forEach(pt -> {
//                            proxyTableMap.put(pt.getPort(), pt);
//                        });
//                // 为所有随机端口的配置生成端口
//                tableList.forEach(pt -> {
//                    if (pt.getIsRandom()) {
//                        // 分配随机端口
//                        int port = RandomPortUtils.getOneRandomPort();
//                        while (proxyTableMap.containsKey(port)) {
//                            port = RandomPortUtils.getOneRandomPort();
//                        }
//                        pt.setPort(port);
//                        proxyTableMap.put(port, pt);
//                    }
//                });
//            }
//        }
//        return proxyTableMap;
    }

    public List<Integer> getServerOutPorts(String appId) {
        return proxyTableMap.values()
                .stream()
                .filter(pt -> pt.getAppId().equals(appId))
                .map(ProxyTable::getPort)
                .distinct()
                .collect(Collectors.toList());
    }

    public synchronized void addProxyTable(ProxyTable proxyTable) {
        String appId = proxyTable.getAppId();
        Integer port = proxyTable.getPort();

        // 检查端口是否被系统占用
        if (proxyTableMap.containsKey(port) || RandomPortUtils.checkIsUsed(port)) {
            throw new ServiceException("端口已被占用");
        }
        proxyTableMap.put(port, proxyTable);

        setChanged();
    }

    public ProxyTable getProxyMapping(Integer port) {
        ProxyTable proxyTable = proxyTableMap.get(port);

        if (proxyTable == null) {
            proxyTable = proxyTableService.getActiveByPort(port);
        }

        return proxyTable;
    }

    public void removeProxyTable(String appId, Integer port) {
        ProxyTable proxyTable = proxyTableMap.get(port);
        if (proxyTable == null) {
            throw new ServiceException("配置信息未找到");
        }
        if (!proxyTable.getAppId().equals(appId)) {
            throw new ServiceException("appId与端口信息不匹配");
        }
        ProxyTable pt = proxyTableMap.remove(port);
        if (pt.getIsUseDomain()) {
            DomainManager.returnDomain(port);
        }
        setChanged();
    }

    public static ProxyTableContainer getInstance() {
        return INSTANCE;
    }

    public List<ProxyTable> getProxyList(String appId) {
        return proxyTableMap()
                .values()
                .stream()
                .filter(p -> p.getAppId().equals(appId))
                .collect(Collectors.toList());
    }

    /**
     * 客户端认证成功后，初始化代理配置
     * @param appId
     * @return
     */
    public List<ProxyTable> initProxyConfig(String appId) {
        List<ProxyTable> tableList = proxyTableService.findActiveList(appId);

        if (!CollectionUtils.isEmpty(tableList)) {
            // 将固定port的配置表先存入map, 避免与随机端口冲突
            tableList.stream()
                    .filter(pt -> !pt.getIsRandom() && !pt.getIsUseDomain())
                    .forEach(pt -> {
                        try {
                            addProxyTable(pt);
                        } catch (ServiceException ignore) {}
                    });

            // 为所有随机端口的配置生成端口
            // 为使用域名的配置生成域名
            tableList.forEach(pt -> {
                if (pt.getIsRandom()) {
                    // 分配随机端口
                    int port = RandomPortUtils.getOneRandomPort();

                    while (proxyTableMap.containsKey(port)) {
                        port = RandomPortUtils.getOneRandomPort();
                    }
                    pt.setPort(port);
                    addProxyTable(pt);
                }
                if (pt.getIsUseDomain()) {
                    // 如果使用域名, 从域名池中预分配域名
                    SysDomain domain = DomainManager.borrowDomain(pt.getIsHttps());
                    if (domain != null) {
                        Integer port = domain.getPort();
                        pt.setPort(port);
                        pt.setDomain(domain.getDomain());
                        pt.setDomainId(domain.getId());
                        addProxyTable(pt);
                    } else {
                        // 分配域名失败
                    }
                }
            });
        }
        return tableList;
    }
}
