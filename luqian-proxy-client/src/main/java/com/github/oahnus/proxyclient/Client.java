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


/**
 * Created by oahnus on 2020-04-01
 * 16:38.
 */
public class Client implements OfflineListener {
    private Bootstrap clientBootstrap;
    private NioEventLoopGroup workerGroup;
    private Bootstrap serverBootstrap;

    private static int reconnectInterval = 2000;

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
        System.out.println("Ready To Connect Proxy Server");
        clientBootstrap.connect(ClientConfig.serverHost, ClientConfig.serverPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    reconnectInterval = 2000;
                    System.out.println("Connect To Server Success, Send Auth Request");
                    NetMessage netMessage = new NetMessage();
                    ClientChannelManager.setCurBridgeChannel(future.channel());
                    netMessage.setType(MessageType.AUTH);
                    netMessage.setUri(ClientConfig.appId + "#" + ClientConfig.appSecret);
                    future.channel().writeAndFlush(netMessage);
                } else {
                    boolean res = waitTime();
                    if (!res) {
                        System.out.println("Connect Timeout");
                        return;
                    }
                    System.out.println("Retry Connect To Server");
                    start();
                }
            }
        });
    }

    @Override
    public void handleOffline() {
        System.out.println("offline");
        try {
            boolean res = waitTime();
            if (!res) {
                System.out.println("Connect Timeout");
                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        start();
    }

    private boolean waitTime() throws InterruptedException {
        System.out.println(String.format("Wait %d ms And Retry", reconnectInterval));
        Thread.sleep(reconnectInterval);
        if (reconnectInterval > 32000) {
            reconnectInterval = 2000;
            return false;
        } else {
            reconnectInterval = reconnectInterval * 2;
        }
        return true;
    }
}
