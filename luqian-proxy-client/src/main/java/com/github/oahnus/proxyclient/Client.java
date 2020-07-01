package com.github.oahnus.proxyclient;

import com.github.oahnus.proxyclient.config.ClientConfig;
import com.github.oahnus.proxyclient.handler.ClientHandler;
import com.github.oahnus.proxyclient.handler.ServiceHandler;
import com.github.oahnus.proxyclient.inter.OfflineListener;
import com.github.oahnus.proxyprotocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;


/**
 * Created by oahnus on 2020-04-01
 * 16:38.
 */
@Slf4j
public class Client implements OfflineListener {
    private Bootstrap clientBootstrap;
    private NioEventLoopGroup workerGroup;
    private Bootstrap serverBootstrap;

    private static int reconnectInterval = 2000;
    private static int retryCount = 0;

    public Client() {
        workerGroup = new NioEventLoopGroup();

        serverBootstrap = new Bootstrap();
        serverBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ServiceHandler());
                    }
                });

        clientBootstrap = new Bootstrap();
        clientBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        initChannelHandler(ch);
                    }});
    }

    private void initChannelHandler(SocketChannel ch) {
        ch.pipeline().addLast(new ProxyProtocolDecoder(1024*1024*10, 0, 4, 0, 0));
        ch.pipeline().addLast(new ProxyProtocolEncoder());
        ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME - 10, 0));
        ch.pipeline().addLast(new ClientHandler(clientBootstrap, serverBootstrap, this));
    }

    public void start() {
        log.info("Ready To Connect Proxy Server");
        clientBootstrap.connect(ClientConfig.serverHost, ClientConfig.serverPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("Connect To Server Success, Send Auth Request");

                    // 重置重连间隔和重连计数
                    reconnectInterval = 2000;
                    retryCount = 0;

                    // 发送认证消息
                    ClientChannelManager.setCurBridgeChannel(future.channel());

                    byte[] payloads = ClientConfig.clientVersion.getBytes(StandardCharsets.UTF_8);
                    NetMessage netMessage = NetMessage.auth(ClientConfig.appId, ClientConfig.appSecret, payloads);

                    future.channel().writeAndFlush(netMessage);
                } else {
                    log.warn("Connect To Server Failed.");
                    waitTime();
                    if (++retryCount > 10) {
                        log.error("Retry Connect To Server Side Over Max Retry Limit");
                        return;
                    }
                    log.info("Retry Connect To Server");
                    start();
                }
            }
        });
    }

    @Override
    public void handleOffline() {
        log.warn("Connection Offline");
        try {
            waitTime();
            if (retryCount++ > 10) {
                log.error("Retry Connect To Server Side Over Max Retry Limit");
                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        start();
    }

    private void waitTime() throws InterruptedException {
        log.info("Wait {} ms And Retry", reconnectInterval);
        Thread.sleep(reconnectInterval);
        if (reconnectInterval > 32000) {
            reconnectInterval = 2000;
        } else {
            reconnectInterval = reconnectInterval * 2;
        }
    }
}
