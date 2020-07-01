package com.github.oahnus.proxyclient;

import com.github.oahnus.proxyclient.config.ClientConfig;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Properties;

/**
 * Created by oahnus on 2020-04-09
 * 6:55.
 */
@Slf4j
public class ClientCmd {
    public static void loadConfigFile() {
        String path = ClientCmd.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {}

        String basePath;
        if (path.contains("\\")) {
            basePath = path.substring(0, path.lastIndexOf("\\") + 1);
        } else {
            basePath = path.substring(0, path.lastIndexOf("/") + 1);
        }

        try {
            Properties properties = new Properties();
            File file = new File(basePath + "/config.cfg");
            if (file.exists()) {
                @Cleanup FileInputStream in = new FileInputStream(file);
                properties.load(in);
            } else {
                @Cleanup FileOutputStream out = new FileOutputStream(file);
                properties.setProperty("host", ClientConfig.defaultHost);
                properties.setProperty("port", ClientConfig.defaultPort);
                properties.setProperty("appId", "");
                properties.setProperty("appSecret", "");

                properties.store(out, "luqian-proxy config file");
            }
            ClientConfig.serverHost = properties.getProperty("host");
            ClientConfig.serverPort = Integer.parseInt(properties.getProperty("port"));
            ClientConfig.appId = properties.getProperty("appId");
            ClientConfig.appSecret = properties.getProperty("appSecret");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error(e.getMessage());
            }
            log.error("Cannot Load Config File, Use Default Config");
        }
    }

    public static void main(String[] args) {
        loadConfigFile();

        Client client = new Client();
        client.start();
    }
}
