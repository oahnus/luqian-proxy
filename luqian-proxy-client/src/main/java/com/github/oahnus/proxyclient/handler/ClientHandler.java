package com.github.oahnus.proxyclient.handler;

import com.github.oahnus.proxyclient.ClientChannelManager;
import com.github.oahnus.proxyclient.inter.CacheStatusListener;
import com.github.oahnus.proxyclient.inter.OfflineListener;
import com.github.oahnus.proxyprotocol.Consts;
import com.github.oahnus.proxyprotocol.MessageType;
import com.github.oahnus.proxyprotocol.NetMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Created by oahnus on 2020-04-01
 * 16:49.
 */
@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<NetMessage> {
    private Bootstrap bootstrap;
    private Bootstrap serverBootstrap;
    private OfflineListener offlineListener;

    public ClientHandler(Bootstrap bootstrap, Bootstrap serverBootstrap, OfflineListener offlineListener) {
        this.bootstrap = bootstrap;
        this.serverBootstrap = serverBootstrap;
        this.offlineListener = offlineListener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NetMessage msg) throws Exception {
        byte type = msg.getType();
        switch (type) {
            case MessageType.CONNECT:
                handleConnect(ctx, msg);
                break;
            case MessageType.PROXY:
                handleForward(ctx, msg);
                break;
            case MessageType.DISCONNECT:
                handleDisconnect(ctx, msg);
                break;
            case MessageType.ERROR:
                handleError(ctx, msg);
                break;
            case MessageType.INFO:
                handleInfoMsg(ctx, msg);
                break;
        }
    }

    private void handleInfoMsg(ChannelHandlerContext ctx, NetMessage msg) {
        String info = new String(msg.getData());
        log.info(info);
    }

    private void handleError(ChannelHandlerContext ctx, NetMessage msg) {
        String errMsg = new String(msg.getData());
        throw new RuntimeException(errMsg);
    }

    private void handleDisconnect(ChannelHandlerContext ctx, NetMessage msg) {
        Channel serviceChannel = ctx.channel().attr(Consts.NEXT_CHANNEL).get();
        if (serviceChannel != null) {
            serviceChannel.attr(Consts.NEXT_CHANNEL).set(null);
            serviceChannel.attr(Consts.APP_ID).set(null);
            serviceChannel.attr(Consts.CHANNEL_ID).set(null);

            ClientChannelManager.returnProxyChannel(ctx.channel());

            serviceChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handleForward(ChannelHandlerContext ctx, NetMessage msg) {
        Channel serviceChannel = ctx.channel().attr(Consts.NEXT_CHANNEL).get();
        if (serviceChannel != null) {
            ByteBuf buf = ctx.alloc().buffer(msg.getData().length);
            buf.writeBytes(msg.getData());
            serviceChannel.writeAndFlush(buf);
        }
    }

    private void handleConnect(ChannelHandlerContext ctx, NetMessage msg) {
        String uri = msg.getUri();
        String appId = uri.split("#")[0];
        String channelId = uri.split("#")[1];

        String hostPort = new String(msg.getData());
        String host = hostPort.split(":")[0];
        int port = Integer.parseInt(hostPort.split(":")[1]);

        Channel bridgeChannel = ctx.channel();
        ClientChannelManager.setCurBridgeChannel(bridgeChannel);

        // 连接本地服务 host, port
        serverBootstrap.connect(host, port).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("Connect Service [{}] Success", hostPort);
                    Channel serviceChannel = future.channel();
                    serviceChannel.config().setOption(ChannelOption.AUTO_READ, false);

                    ClientChannelManager.getCachedProxyChannel(bootstrap, new CacheStatusListener() {
                        @Override
                        public void success(Channel proxyChannel) {
                            // 构建双向channel
                            proxyChannel.attr(Consts.NEXT_CHANNEL).set(serviceChannel);
                            serviceChannel.attr(Consts.NEXT_CHANNEL).set(proxyChannel);
                            // 绑定channelId
                            proxyChannel.attr(Consts.CHANNEL_ID).set(channelId);
                            serviceChannel.attr(Consts.CHANNEL_ID).set(channelId);

                            serviceChannel.attr(Consts.APP_ID).set(appId);

                            ClientChannelManager.addServiceChannel(channelId, serviceChannel);

                            NetMessage msg = NetMessage.connect(appId, channelId, null);
                            proxyChannel.writeAndFlush(msg);
                            serviceChannel.config().setOption(ChannelOption.AUTO_READ, true);
                        }

                        @Override
                        public void fail() {
                            NetMessage message = NetMessage.disconnect(appId, channelId, null);
                            bridgeChannel.writeAndFlush(message);
                        }
                    });
                } else {
                    // disconnect
                    NetMessage msg = NetMessage.disconnect(appId, channelId, null);
                    bridgeChannel.writeAndFlush(msg);
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (ClientChannelManager.getCurBridgeChannel() == ctx.channel()) {
            // 命令通道断开连接
            ClientChannelManager.clearCurBridgeChannel();
            ClientChannelManager.clearProxyChannelPool();

            offlineListener.handleOffline();
        } else {
            // 服务通道断开连接
            Channel serviceChannel = ctx.channel().attr(Consts.NEXT_CHANNEL).get();
            if (serviceChannel != null && serviceChannel.isActive()) {
                serviceChannel.close();
            }
        }

        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel serverChannel = ctx.channel().attr(Consts.NEXT_CHANNEL).get();
        if (serverChannel != null) {
            serverChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            log.error("Connection Break.连接断开");
        } else if(cause instanceof RuntimeException) {
            System.err.println(cause.getMessage());
            log.error(cause.getMessage());

            ctx.channel().close();

            if (cause.getMessage().startsWith("AppId Conflicted")) {
                System.exit(0);
            }
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }
}
