package com.github.oahnus.proxyserver.bootstrap;

import com.github.oahnus.proxyserver.handler.web.HttpServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Created by oahnus on 2020-03-31
 * 14:50.
 */
public class WebServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap serverBootstrap;

    private static class SingletonWebServer {
        static WebServer instance = new WebServer();
    }

    public static WebServer getInstance() {
        return SingletonWebServer.instance;
    }

    public WebServer() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) throws Exception {
                        ChannelPipeline pipeline = sc.pipeline();
                        // Encoder Decoder

                        /* 解析Http请求 */
                        pipeline.addLast(new HttpServerCodec());  //Outbound
                        //主要是将同一个http请求或响应的多个消息对象变成一个 fullHttpRequest完整的消息对象
                        pipeline.addLast(new HttpObjectAggregator(64 * 1024));//Inbound
                        //主要用于处理大数据流,比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的 ,加上这个handler我们就不用考虑这个问题了
                        pipeline.addLast(new ChunkedWriteHandler());//Inbound、Outbound
                        pipeline.addLast(new HttpServerHandler());
                    }
                });

    }

    public void start(){
        ChannelFuture future = serverBootstrap.bind(7777);
        System.out.println("netty server Start Listening On " + 7777);
    }
}
