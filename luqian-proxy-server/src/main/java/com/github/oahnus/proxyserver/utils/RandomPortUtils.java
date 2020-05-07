package com.github.oahnus.proxyserver.utils;

import com.github.oahnus.luqiancommon.generate.SnowFlake;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

/**
 * Created by oahnus on 2020-04-28
 * 10:20.
 */
public class RandomPortUtils {
    public static final int RANDOM_PORT_MIN = 12000;
    public static final int RANDOM_PORT_MAX_COUNT = 2000;

    private static Random random = new Random();

    public static int getOneRandomPort() {
        int port = random.nextInt(RANDOM_PORT_MAX_COUNT) + RANDOM_PORT_MIN;
        // 检查port是否被系统应用占用
        while (checkIsUsed(port)) {
            port = random.nextInt(RANDOM_PORT_MAX_COUNT) + RANDOM_PORT_MIN;
        }
        return port;
    }

    public static boolean checkIsUsed(int port) {
        boolean isUsed = false;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.close();
        } catch (IOException e) {
            isUsed = true;
        }
        return isUsed;
    }

    public static void main(String... args) {
//        int port = getOneRandomPort();
//        System.out.println(port);
        SnowFlake snowFlake = new SnowFlake(12, 2);
        System.out.println(snowFlake.generateId());
    }
}
