package com.github.oahnus.proxyclient;

import com.github.oahnus.proxyclient.config.ClientConfig;
import com.github.oahnus.proxyclient.inter.CacheStatusListener;
import com.github.oahnus.proxyprotocol.Consts;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by oahnus on 2020-04-07
 * 17:22.
 */
public class ClientChannelManager {
    private static Channel bridgeChannel = null;
    private static Queue<Channel> PROXY_CHANNEL_POOL = new ConcurrentLinkedQueue<>();
    private static final int MAX_POOL_SIZE = 50;
    // key -> channelId
    private static Map<String, Channel> SERVICE_CHANNEL_MAPPINGS = new ConcurrentHashMap<>();

    public static void addServiceChannel(String channelId, Channel channel) {
        SERVICE_CHANNEL_MAPPINGS.put(channelId, channel);
    }
    public static void removeServiceChannel(String channelId) {
        SERVICE_CHANNEL_MAPPINGS.remove(channelId);
    }
    public static Channel getServiceChannel(String channelId) {
        return SERVICE_CHANNEL_MAPPINGS.get(channelId);
    }

    public static void setCurBridgeChannel(Channel channel) {
        bridgeChannel = channel;
    }

    public static Channel getCurBridgeChannel() {
        return bridgeChannel;
    }

    public static void clearCurBridgeChannel() {
        bridgeChannel = null;
    }

    public static void getCachedProxyChannel(Bootstrap bootstrap, CacheStatusListener listener) {
        Channel proxyChannel = PROXY_CHANNEL_POOL.poll();
        if (proxyChannel == null) {
            bootstrap.connect(ClientConfig.serverHost, ClientConfig.serverPort).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        listener.success(future.channel());
                    } else {
                        listener.fail();
                    }
                }
            });
        } else {
            listener.success(proxyChannel);
        }
    }

    public static void returnProxyChannel(Channel proxyChannel) {
        if (PROXY_CHANNEL_POOL.size() < MAX_POOL_SIZE) {
            PROXY_CHANNEL_POOL.offer(proxyChannel);
        } else {
            proxyChannel.attr(Consts.APP_ID).set(null);
            proxyChannel.attr(Consts.CHANNEL_ID).set(null);
            proxyChannel.attr(Consts.NEXT_CHANNEL).set(null);
            proxyChannel.close();
        }
    }

    public static void clearProxyChannelPool() {
        for (Channel serviceChannel : PROXY_CHANNEL_POOL) {
            if (serviceChannel.isActive()) {
                serviceChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                serviceChannel.close();
            }
        }
        PROXY_CHANNEL_POOL.clear();
    }
}
