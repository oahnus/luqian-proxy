package com.github.oahnus.proxyclient;

import com.github.oahnus.proxyclient.config.ClientConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by oahnus on 2020-04-09
 * 6:55.
 */
public class ClientCmd {
    public static void loadConfig() {
        Properties prop = new Properties();
        InputStream in = ClientCmd.class.getClassLoader().getResourceAsStream("config.properties");
        try {
            prop.load(in);
            ClientConfig.serverHost = (String) prop.getOrDefault("server.host", "127.0.0.1");
            ClientConfig.serverPort = Integer.valueOf((String) prop.getOrDefault("server.port", 7766));
            ClientConfig.appId = (String) prop.getOrDefault("app.id", "");
            ClientConfig.appSecret = (String) prop.getOrDefault("app.secret", "");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("config file load error");
        }
    }

    public static void main(String[] args) {
        loadConfig();

        Client client = new Client();
        client.start();
    }
}
