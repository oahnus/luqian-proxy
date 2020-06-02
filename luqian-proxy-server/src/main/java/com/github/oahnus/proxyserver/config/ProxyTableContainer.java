package com.github.oahnus.proxyserver.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.exceptions.ServiceException;
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
    private static AtomicInteger version = new AtomicInteger();
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

    public static int getVersion() {
        return version.get();
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
        version.incrementAndGet();
        return applicationMap.put(appId, appSecret);
    }

    public Map<Integer, ProxyTable> proxyTableMap() {
        if (proxyTableMap.isEmpty()) {
            // 如果map为空，尝试从数据库读取proxy配置
            List<ProxyTable> tableList = proxyTableService.loadAllActive();
            if (!CollectionUtils.isEmpty(tableList)) {
                // 将固定port的配置表先存入map, 避免与随机端口冲突
                tableList.stream()
                        .filter(pt -> !pt.getIsRandom())
                        .forEach(pt -> {
                            proxyTableMap.put(pt.getPort(), pt);
                        });
                // 为所有随机端口的配置生成端口
                tableList.forEach(pt -> {
                    if (pt.getIsRandom()) {
                        // 分配随机端口
                        int port = RandomPortUtils.getOneRandomPort();
                        while (proxyTableMap.containsKey(port)) {
                            port = RandomPortUtils.getOneRandomPort();
                        }
                        pt.setPort(port);
                        proxyTableMap.put(port, pt);
                    }
                });
            }
        }
        return proxyTableMap;
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
        version.incrementAndGet();
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
        proxyTableMap.remove(port);
        setChanged();
        version.incrementAndGet();
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
}
