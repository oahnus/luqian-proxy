package com.github.oahnus.proxyserver.bootstrap;

import com.github.oahnus.proxyprotocol.IdleCheckHandler;
import com.github.oahnus.proxyprotocol.ProxyProtocolDecoder;
import com.github.oahnus.proxyprotocol.ProxyProtocolEncoder;
import com.github.oahnus.proxyserver.config.ProxyTable;
import com.github.oahnus.proxyserver.entity.ProxyTableItem;
import com.github.oahnus.proxyserver.handler.proxy.ForwardHandler;
import com.github.oahnus.proxyserver.handler.proxy.ProxyServerHandler;
import com.github.oahnus.proxyserver.handler.stat.StatisticsHandler;
import com.github.oahnus.proxyserver.manager.ServerChannelManager;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oahnus on 2020-03-31
 * 14:37.
 */
@Component
public class ProxyServer implements Observer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap serverBootstrap;

    private static Map<Integer, ChannelFuture> futureMap = new ConcurrentHashMap<>();

    // 单例
    private static class SingletonProxyServer {
        static ProxyServer instance = new ProxyServer();
    }
    public static ProxyServer getInstance() {
        return SingletonProxyServer.instance;
    }

    public ProxyServer() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) throws Exception {
                        ChannelPipeline pipeline = sc.pipeline();
                        // Encoder Decoder
                        pipeline.addLast(new ProxyProtocolDecoder(1024*1024*10, 0, 4, 0, 0));
                        pipeline.addLast(new ProxyProtocolEncoder());
                        pipeline.addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, 0));
                        pipeline.addLast(new ProxyServerHandler());
                    }
                });
    }

    public void start() throws InterruptedException {
        ChannelFuture future = serverBootstrap.bind(7766).sync();
        if (future.isSuccess()) {
            System.out.println("success");
        }
        System.out.println("netty server Start Listening On " + 7766);

        ServerBootstrap forwardBootstrap = new ServerBootstrap();
        forwardBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ForwardHandler());
            }
        });

        try {
            ProxyTable.loadFromDisk();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Load Config Error");
        }

        // 将ProxyServer添加为观察者, 监听代理配置信息的改变
        ProxyTable.getInstance().addObserver(this);

        // 启动Server
        startForwardServer();
    }

    @Override
    public void update(Observable o, Object arg) {
        ProxyTable proxyTable = (ProxyTable) o;
        // 配置信息已修改
        startForwardServer();
    }

    private void startForwardServer() {
        Map<Integer, ProxyTableItem> proxyTableItemMap = ProxyTable.getInstance().proxyTableItemMap();
        // 关闭失效端口
        for (Integer port : futureMap.keySet()) {
            if (!proxyTableItemMap.containsKey(port)) {
                // 端口 port 已取消映射
                ChannelFuture future = futureMap.get(port);
                future.channel().close();
                // 移除端口和命令Channel的映射
                TrafficMeasureMonitor.removeMeasure(port);
                ServerChannelManager.removePort2BridgeChannelMapping(port);
            }
        }

        for (Map.Entry<Integer, ProxyTableItem> entry : proxyTableItemMap.entrySet()) {
            Integer port = entry.getKey();
            ProxyTableItem tableItem = entry.getValue();
            String appId = tableItem.getAppId();
            String serviceAddr = tableItem.getServiceAddr();

            // 检查是否已监听端口
            if (ServerChannelManager.getPort2BridgeChannelMapping(port) != null) {
                continue;
            }
            // 取当前用户已有命令channel(bridgeChannel), 添加新端口和命令channel的映射
            Channel bridgeChannel = ServerChannelManager.getBridgeChannel(appId);
            if (bridgeChannel != null) {
                ServerChannelManager.addPort2BridgeChannelMapping(port, bridgeChannel);
            }

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addFirst(new StatisticsHandler());
                            ch.pipeline().addLast(new ForwardHandler());
                        }
                    });
            try {
                TrafficMeasureMonitor.createStatMeasure(appId, port);

                ChannelFuture future = bootstrap.bind(port).sync();
                futureMap.put(port, future);

                System.out.println("Bind Outside Port On " + port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
