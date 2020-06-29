package com.github.oahnus.proxyserver.handler.proxy;

import com.github.oahnus.luqiancommon.util.encrypt.AESUtils;
import com.github.oahnus.proxyprotocol.Consts;
import com.github.oahnus.proxyprotocol.MessageType;
import com.github.oahnus.proxyprotocol.NetMessage;
import com.github.oahnus.proxyserver.config.ProxyTableContainer;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.entity.SysDomain;
import com.github.oahnus.proxyserver.manager.DomainManager;
import com.github.oahnus.proxyserver.manager.ServerChannelManager;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oahnus on 2020-03-31
 * 14:53.
 */
@Slf4j
public class ProxyServerHandler extends SimpleChannelInboundHandler<NetMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NetMessage msg) throws Exception {
        byte type = msg.getType();
        switch (type) {
            case MessageType.CONNECT:
                handleConnectMessage(ctx, msg);
                break;
            case MessageType.AUTH:
                handleAuthMessage(ctx, msg);
                break;
            case MessageType.HEARTBEAT:
                handleHeartBeat(ctx, msg);
                break;
            case MessageType.PROXY:
                handleProxyMessage(ctx, msg);
                break;
            case MessageType.DISCONNECT:
                handleDisconnect(ctx, msg);
                break;
        }
    }

    private void handleHeartBeat(ChannelHandlerContext ctx, NetMessage msg) {
        NetMessage message = new NetMessage();
        message.setType(MessageType.HEARTBEAT);
        ctx.channel().writeAndFlush(message);
    }

    private void handleDisconnect(ChannelHandlerContext ctx, NetMessage msg) {
        String appId = ctx.channel().attr(Consts.APP_ID).get();

        if (StringUtils.isEmpty(appId)) {
            String uri = msg.getUri();
            String channelId = uri.split("#")[1];

            // auth 请求连接断开
            Map<String, Channel> outsideChannels = ctx.channel().attr(Consts.OUTSIDE_CHANNELS).get();
            if (outsideChannels == null) {
                return;
            }
            Channel outChannel = outsideChannels.remove(channelId);
            if (outChannel != null) {
                outChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
            return;
        }
        Channel bridgeChannel = ServerChannelManager.getBridgeChannel(appId);
        if (bridgeChannel == null) {
            return;
        }
        String channelId = ctx.channel().attr(Consts.CHANNEL_ID).get();
        Map<String, Channel> outChannels = bridgeChannel.attr(Consts.OUTSIDE_CHANNELS).get();
        if (outChannels == null) {
            return;
        }
        // ChannelId NPE
        if (channelId == null) {
            return;
        }
        Channel outChannel = outChannels.remove(channelId);
        if (outChannel != null) {
            outChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            ctx.channel().attr(Consts.NEXT_CHANNEL).set(null);
            ctx.channel().attr(Consts.CHANNEL_ID).set(null);
            ctx.channel().attr(Consts.APP_ID).set(null);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel outChannel = ctx.channel().attr(Consts.NEXT_CHANNEL).get();
        if (outChannel != null) {
            outChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }
        super.channelWritabilityChanged(ctx);
    }

    private void handleProxyMessage(ChannelHandlerContext ctx, NetMessage msg) {
        Channel outChannel = ctx.channel().attr(Consts.NEXT_CHANNEL).get();
        if (outChannel != null) {
            ByteBuf buf = ctx.alloc().buffer(msg.getData().length);
            buf.writeBytes(msg.getData());
            outChannel.writeAndFlush(buf);
        }
    }

    /**
     * 客户端认证消息
     */
    private void handleAuthMessage(ChannelHandlerContext ctx, NetMessage msg) {
        String authString = msg.getUri();
        String[] strings = authString.split("#");
        String appId = strings[0];
        String appSecret = strings[1];

        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(appSecret)) {
            ctx.close();
            return;
        }

        String secret = ProxyTableContainer.getInstance().getAppSecret(appId);
        if (secret == null) {
            NetMessage netMessage = new NetMessage();
            netMessage.setType(MessageType.ERROR);
            netMessage.setData("AppId Is Not Existed".getBytes());
            ctx.channel().writeAndFlush(netMessage);
            ctx.channel().close();
            return;
        }
        String decrypt = AESUtils.decrypt(appSecret);
        if (!decrypt.equals(appId)) {
            // 如果appSecret与appId无法匹配
            NetMessage netMessage = new NetMessage();
            netMessage.setType(MessageType.ERROR);
            netMessage.setData("Authenticate Failed. AppSecret Is Invalid".getBytes());
            ctx.channel().writeAndFlush(netMessage);
            ctx.channel().close();
            return;
        }

        // TODO 检查客户端版本
        byte[] data = msg.getData();
        String clientVersion = data == null ? null : new String(data, StandardCharsets.UTF_8);

        Channel bridgeChannel = ServerChannelManager.getBridgeChannel(appId);
        if (bridgeChannel != null) {
            // 如果appId已经登录在server端, 向就旧的client发送断线消息
            NetMessage netMessage = new NetMessage();
            netMessage.setType(MessageType.ERROR);
            netMessage.setData("AppId Conflicted. This AppId Has Been Authenticated By Other Client".getBytes());
            bridgeChannel.writeAndFlush(netMessage);
            bridgeChannel.close();
        }

        // 认证成功， 初始化代理配置
        List<ProxyTable> proxyTableList = ProxyTableContainer.getInstance().initProxyConfig(appId);
        // init 流量监控
        proxyTableList.forEach(TrafficMeasureMonitor::createStatMeasure);

        List<Integer> serverOutPorts = ProxyTableContainer.getInstance().getServerOutPorts(appId);
        for (Integer port : serverOutPorts) {
            ServerChannelManager.addPort2BridgeChannelMapping(port, ctx.channel());
        }
        ctx.channel().attr(Consts.OUTSIDE_PORTS).set(serverOutPorts);
        ctx.channel().attr(Consts.APP_ID).set(appId);
        ctx.channel().attr(Consts.OUTSIDE_CHANNELS).set(new ConcurrentHashMap<>());
        ServerChannelManager.addBridgeChannel(appId, ctx.channel());

        // 发送认证成功消息
        String retMsg = "Authenticate Success.";

        NetMessage netMessage = new NetMessage();
        netMessage.setType(MessageType.INFO);
        netMessage.setData(retMsg.getBytes());
        ctx.channel().writeAndFlush(netMessage);

        // 刷新配置
        ProxyTableContainer.getInstance().notifyObservers(appId);
    }

    private void handleConnectMessage(ChannelHandlerContext ctx, NetMessage msg) {
        String uri = msg.getUri();
        if (StringUtils.isEmpty(uri)) {
            ctx.close();
            return;
        }
        String appId = uri.split("#")[0];
        String channelId = uri.split("#")[1];

        Channel bridgeChannel = ServerChannelManager.getBridgeChannel(appId);
        if (bridgeChannel == null) {
            ctx.channel().close();
            return;
        }
        Channel outChannel = bridgeChannel.attr(Consts.OUTSIDE_CHANNELS).get().get(channelId);
        if (outChannel != null) {
            // ctx.channel() <=> proxyChannel
            outChannel.attr(Consts.NEXT_CHANNEL).set(ctx.channel());
            outChannel.attr(Consts.CHANNEL_ID).set(channelId);
            outChannel.attr(Consts.APP_ID).set(appId);
            ctx.channel().attr(Consts.NEXT_CHANNEL).set(outChannel);
            ctx.channel().attr(Consts.APP_ID).set(appId);
            ctx.channel().attr(Consts.CHANNEL_ID).set(channelId);
            outChannel.config().setOption(ChannelOption.AUTO_READ, true);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel outChannel = ctx.channel().attr(Consts.NEXT_CHANNEL).get();

        if (outChannel != null && outChannel.isActive()) {
            // 转发端断开连接
            String appId = ctx.channel().attr(Consts.APP_ID).get();
            String channelId = ctx.channel().attr(Consts.CHANNEL_ID).get();
            Channel bridgeChannel = ServerChannelManager.getBridgeChannel(appId);
            if (bridgeChannel != null) {
                bridgeChannel.attr(Consts.OUTSIDE_CHANNELS).get().remove(channelId);
            } else {

            }

            // 数据发送完成后再关闭连接，解决http1.0数据传输问题
            outChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            outChannel.close();
        } else {
            // 客户端断开连接
            if (ctx.channel().attr(Consts.OUTSIDE_PORTS).get() == null) {
                return;
            }

            String appId = ctx.channel().attr(Consts.APP_ID).get();

            Channel channel = ServerChannelManager.removeBridgeChannel(appId);
            if (channel != null && ctx.channel() != channel) {
                ServerChannelManager.addBridgeChannel(appId, ctx.channel());
            }

            if (channel != null) {
                List<Integer> ports = channel.attr(Consts.OUTSIDE_PORTS).get();
                for (int port : ports) {
                    Channel proxyChannel = ServerChannelManager.removePort2BridgeChannelMapping(port);
                    if (proxyChannel == null) {
                        continue;
                    }

                    // 在执行断连之前新的连接已经连上来了
                    if (proxyChannel != channel) {
                        ServerChannelManager.addPort2BridgeChannelMapping(port, proxyChannel);
                    } else {
                        // 移除代理配置
                        ProxyTableContainer.getInstance().removeProxyTable(appId, port);
                    }
                }

                if (channel.isActive()) {
                    channel.close();
                }
            }
            // 刷新server
            ProxyTableContainer.getInstance().notifyObservers();

            // 断开 开放端口的连接
            Map<String, Channel> userChannels = ctx.channel().attr(Consts.OUTSIDE_CHANNELS).get();
            for (String channelId : userChannels.keySet()) {
                Channel userChannel = userChannels.get(channelId);
                if (userChannel.isActive()) {
                    userChannel.close();
                }
            }
        }

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            log.info("Connection Break");
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }
}
