package com.github.oahnus.proxyserver.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.oahnus.proxyserver.entity.ProxyTableItem;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oahnus on 2020-04-03
 * 15:14.
 */
public class ProxyTable extends Observable {
    private static AtomicInteger version = new AtomicInteger();
    private static Map<Integer, ProxyTableItem> proxyTableMap = new ConcurrentHashMap<>();

    private static Map<String, AtomicInteger> clientCounter = new ConcurrentHashMap<>();

    private static Map<String, String> users = new ConcurrentHashMap<>();

    private static ProxyTable INSTANCE;

    private ProxyTable() {}

    public static int getVersion() {
        return version.get();
    }

    public static void saveToDisk() throws IOException {
        System.out.println("Save Config");
        File file = new File("server.json");
        OutputStream out = new FileOutputStream(file);
        String s1 = JSON.toJSONString(proxyTableMap);
        String s2 = JSON.toJSONString(clientCounter);
        String s3 = JSON.toJSONString(users);

        String s = s1 + "#####" + s2 + "#####" + s3;
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
        if (strings.length != 3) {
            return;
        }
        proxyTableMap = JSON.parseObject(strings[0], new TypeReference<Map<Integer, ProxyTableItem>>() {});
        clientCounter = JSON.parseObject(strings[1], new TypeReference<Map<String, AtomicInteger>>() {});
        users = JSON.parseObject(strings[2], new TypeReference<Map<String, String>>() {});
        in.close();
    }

    public String getAppSecret(String appId) {
        return users.get(appId);
    }

    public synchronized String addUser(String appId, String appSecret) {
        version.incrementAndGet();
        return users.put(appId, appSecret);
    }

    public Map<Integer, ProxyTableItem> proxyTableItemMap() {
        return proxyTableMap;
    }

    public List<Integer> getServerOutPorts() {
        return new ArrayList<>(proxyTableMap.keySet());
    }

    public synchronized boolean addOutPort(String appId, Integer port) {
        AtomicInteger counter = clientCounter.get(appId);
        if (counter == null) {
            counter = new AtomicInteger();
            clientCounter.put(appId, counter);
        }
        int count = counter.incrementAndGet();
        if (count > 3) {
            return false;
        }
        ProxyTableItem item = new ProxyTableItem();
        item.setAppId(appId);
        item.setPort(port);
        proxyTableMap.put(port, item);

        setChanged();
        version.incrementAndGet();
        return true;
    }

    public ProxyTableItem getProxyMapping(Integer port) {
        return proxyTableMap.get(port);
    }

    public void addPort2ServiceMapping(Integer port, String hostPort) {
        ProxyTableItem tableItem = proxyTableMap.get(port);
        tableItem.setServiceAddr(hostPort);
        proxyTableMap.put(port, tableItem);
        version.incrementAndGet();
        setChanged();
    }

    public void removeMapping(Integer port) {
        proxyTableMap.remove(port);
        setChanged();
        version.incrementAndGet();
    }

    public static ProxyTable getInstance() {
        if (INSTANCE == null) {
            synchronized (ProxyTable.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ProxyTable();
                }
            }
        }
        return INSTANCE;
    }
}
