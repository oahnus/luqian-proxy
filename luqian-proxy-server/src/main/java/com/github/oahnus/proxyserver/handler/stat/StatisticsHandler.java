package com.github.oahnus.proxyserver.handler.stat;

import com.github.oahnus.proxyprotocol.Consts;
import com.github.oahnus.proxyprotocol.NetMessage;
import com.github.oahnus.proxyserver.manager.ServerChannelManager;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.net.InetSocketAddress;

/**
 * Created by oahnus on 2020-04-09
 * 13:28.
 */
public class StatisticsHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        int port = socketAddress.getPort();

        int byteLen = ((ByteBuf) msg).readableBytes();

        String appId = ctx.channel().attr(Consts.APP_ID).get();

        // 记录入网流量, 如果流量超过限制, 断开连接
        boolean res = TrafficMeasureMonitor.addInTrafficBytes(port, byteLen);
        if (res) {
            ctx.fireChannelRead(msg);
        } else {
            Channel bridgeChannel = ServerChannelManager.getPort2BridgeChannelMapping(port);

            NetMessage netMessage = NetMessage.notify("Traffic Amount Overflow.流量额度已用完");
            bridgeChannel.writeAndFlush(netMessage);
            ctx.channel().close();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        int port = socketAddress.getPort();

        int byteLen = ((ByteBuf) msg).readableBytes();
        String appId = ctx.channel().attr(Consts.APP_ID).get();

        TrafficMeasureMonitor.addOutTrafficBytes(port, byteLen);
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        int port = socketAddress.getPort();
        String appId = ctx.channel().attr(Consts.APP_ID).get();

        TrafficMeasureMonitor.incrConnectCount(port);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        int port = socketAddress.getPort();
        String appId = ctx.channel().attr(Consts.APP_ID).get();

        TrafficMeasureMonitor.decrConnectCount(port);

        super.channelInactive(ctx);
    }
}
