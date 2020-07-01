package com.github.oahnus.proxyserver.components;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oahnus on 2020-06-30
 */
public class ServerCache {
    private static Map<String, Object> CACHE = new HashMap<>(16);

    public static void put(String key,  Object val) {
        CACHE.put(key, val);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        Object obj = CACHE.get(key);
        T val = (T) obj;
        return val;
    }
}
