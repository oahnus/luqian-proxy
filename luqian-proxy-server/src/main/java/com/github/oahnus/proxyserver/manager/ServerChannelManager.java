package com.github.oahnus.proxyserver.manager;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oahnus on 2020-04-07
 * 12:52.
 */
public class ServerChannelManager {
    // key -> appId 用户id
    private static Map<String, Channel> bridgeChannels = new ConcurrentHashMap<>();
    // key -> port 转发端口
    private static Map<Integer, Channel> outsidePort2BridgeChannelMapping = new ConcurrentHashMap<>();

    public static Channel getBridgeChannel(String appId) {
        return bridgeChannels.get(appId);
    }

    public static void addBridgeChannel(String appId, Channel channel) {
        bridgeChannels.put(appId, channel);
    }

    public static Channel removeBridgeChannel(String appId) {
        return bridgeChannels.remove(appId);
    }

    public static void addPort2BridgeChannelMapping(Integer outsidePort, Channel channel) {
        outsidePort2BridgeChannelMapping.put(outsidePort, channel);
    }

    public static Channel getPort2BridgeChannelMapping(Integer outsidePort) {
        return outsidePort2BridgeChannelMapping.get(outsidePort);
    }
    public static Channel removePort2BridgeChannelMapping(Integer outsidePort) {
        return outsidePort2BridgeChannelMapping.remove(outsidePort);
    }
}
