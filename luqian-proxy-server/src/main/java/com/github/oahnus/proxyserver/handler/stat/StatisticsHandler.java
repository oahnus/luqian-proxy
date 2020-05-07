package com.github.oahnus.proxyserver.handler.stat;

import com.github.oahnus.proxyprotocol.Consts;
import com.github.oahnus.proxyserver.entity.StatMeasure;
import com.github.oahnus.proxyserver.manager.TrafficMeasureMonitor;
import io.netty.buffer.ByteBuf;
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

        StatMeasure measure = TrafficMeasureMonitor.getStatMeasure(port);
        boolean res = measure.addInTrafficBytes(byteLen);
        if (res) {
            ctx.fireChannelRead(msg);
        } else {
            ctx.channel().close();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        int port = socketAddress.getPort();

        int byteLen = ((ByteBuf) msg).readableBytes();
        String appId = ctx.channel().attr(Consts.APP_ID).get();

        StatMeasure measure = TrafficMeasureMonitor.getStatMeasure(port);
        measure.addOutTrafficBytes(byteLen);

        super.write(ctx, msg, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        int port = socketAddress.getPort();
        String appId = ctx.channel().attr(Consts.APP_ID).get();

        StatMeasure measure = TrafficMeasureMonitor.getStatMeasure(port);
        measure.incrConnectCount();

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        int port = socketAddress.getPort();
        String appId = ctx.channel().attr(Consts.APP_ID).get();

        StatMeasure measure = TrafficMeasureMonitor.getStatMeasure(port);
        measure.decrConnectCount();

        super.channelInactive(ctx);
    }
}
