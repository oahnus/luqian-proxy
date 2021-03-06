package com.github.oahnus.proxyclient.handler;

import com.github.oahnus.proxyclient.ClientChannelManager;
import com.github.oahnus.proxyprotocol.Consts;
import com.github.oahnus.proxyprotocol.MessageType;
import com.github.oahnus.proxyprotocol.NetMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by oahnus on 2020-04-03
 * 7:25.
 * 与
 */
@Slf4j
public class ServiceHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        log.info("Forward {} Bytes Data From Channel", buf.readableBytes());
        Channel serviceChannel = ctx.channel();
        Channel proxyChannel = serviceChannel.attr(Consts.NEXT_CHANNEL).get();

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        if (log.isDebugEnabled()) {
            log.debug("Data: {}", new String(bytes));
        }

        String channelId = serviceChannel.attr(Consts.CHANNEL_ID).get();
        String appId = serviceChannel.attr(Consts.APP_ID).get();

        NetMessage msg = NetMessage.proxy(appId, channelId, bytes);
        proxyChannel.writeAndFlush(msg);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel serverChannel = ctx.channel();
        Channel bridgeChannel = serverChannel.attr(Consts.NEXT_CHANNEL).get();
        if (bridgeChannel != null) {
            bridgeChannel.config().setOption(ChannelOption.AUTO_READ, serverChannel.isWritable());
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel serviceChannel = ctx.channel();
        String appId = serviceChannel.attr(Consts.APP_ID).get();
        String channelId = serviceChannel.attr(Consts.CHANNEL_ID).get();
        log.debug("Channel Inactive, ChannelId = {}", channelId);
        if (channelId != null) {
            ClientChannelManager.removeServiceChannel(channelId);
        }

        Channel proxyChannel = serviceChannel.attr(Consts.NEXT_CHANNEL).get();
        if (proxyChannel != null) {
            NetMessage proxyMessage = NetMessage.disconnect(appId, channelId, null);
            proxyChannel.writeAndFlush(proxyMessage);

            proxyChannel.attr(Consts.APP_ID).set(appId);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
