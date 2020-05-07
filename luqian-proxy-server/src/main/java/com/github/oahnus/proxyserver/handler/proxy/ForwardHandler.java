package com.github.oahnus.proxyserver.handler.proxy;

import com.github.oahnus.proxyprotocol.Consts;
import com.github.oahnus.proxyprotocol.MessageType;
import com.github.oahnus.proxyprotocol.NetMessage;
import com.github.oahnus.proxyserver.config.ProxyTableContainer;
import com.github.oahnus.proxyserver.entity.ProxyTable;
import com.github.oahnus.proxyserver.manager.ServerChannelManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;

/**
 * Created by oahnus on 2020-04-01
 * 11:38.
 * 服务端处理对外开放 代理接口的请求
 */
public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        Channel outChannel = ctx.channel();
        Channel proxyChannel = outChannel.attr(Consts.NEXT_CHANNEL).get();
        if (proxyChannel == null) {
            ctx.channel().close();
            return;
        }
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String channelId = outChannel.attr(Consts.CHANNEL_ID).get();
        String appId = outChannel.attr(Consts.APP_ID).get();
        NetMessage message = new NetMessage();
        message.setType(MessageType.PROXY);
        message.setUri(appId + "#" + channelId);
        message.setData(bytes);
        proxyChannel.writeAndFlush(message);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 用户连接服务端对外开放的端口时
        Channel outChannel = ctx.channel();
        InetSocketAddress inetSocketAddress = (InetSocketAddress) outChannel.localAddress();

        int port = inetSocketAddress.getPort();

        // 根据访问的开放端口获取命令通道
        Channel bridgeChannel = ServerChannelManager.getPort2BridgeChannelMapping(port);
        if (bridgeChannel == null) {
            ctx.channel().close();
            return;
        }
        // 设置连接不可读
        outChannel.config().setOption(ChannelOption.AUTO_READ, false);
        // 查找开放接口对应内网的host:port地址
        ProxyTable tableItem = ProxyTableContainer.getInstance().getProxyMapping(port);
        String hostPort = tableItem.getServiceAddr();

        String channelId = UUID.randomUUID().toString();
        bridgeChannel.attr(Consts.CHANNEL_ID).set(channelId);
        bridgeChannel.attr(Consts.OUTSIDE_CHANNELS).get().put(channelId, outChannel);

        String appId = bridgeChannel.attr(Consts.APP_ID).get();

        // 发送连接通知到client
        NetMessage message = new NetMessage();
        message.setType(MessageType.CONNECT);
        message.setUri(appId + "#" + channelId);
        message.setData(hostPort.getBytes());
        bridgeChannel.writeAndFlush(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel outChannel = ctx.channel();

        InetSocketAddress inetSocketAddress = (InetSocketAddress) outChannel.localAddress();
        int port = inetSocketAddress.getPort();

        Channel bridgeChannel = ServerChannelManager.getPort2BridgeChannelMapping(port);
        if (bridgeChannel == null) {
            outChannel.close();
            return;
        }

        String channelId = bridgeChannel.attr(Consts.CHANNEL_ID).get();
        String appId = bridgeChannel.attr(Consts.APP_ID).get();

        Map<String, Channel> outChannels = bridgeChannel.attr(Consts.OUTSIDE_CHANNELS).get();
        if (outChannels != null) {
            outChannels.remove(channelId);
        }
        Channel proxyChannel = outChannel.attr(Consts.NEXT_CHANNEL).get();
        if (proxyChannel != null) {
            proxyChannel.attr(Consts.NEXT_CHANNEL).set(null);
            proxyChannel.attr(Consts.CHANNEL_ID).set(null);
            proxyChannel.attr(Consts.APP_ID).set(null);

            proxyChannel.config().setOption(ChannelOption.AUTO_READ, true);
            // 通知客户端，用户连接已经断开
            NetMessage msg = new NetMessage();
            msg.setType(MessageType.DISCONNECT);
            msg.setUri(appId + "#" + channelId);
            proxyChannel.writeAndFlush(msg);
        }

        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // 通知代理客户端
        Channel outChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) outChannel.localAddress();
        Channel bridgeChannel = ServerChannelManager.getPort2BridgeChannelMapping(sa.getPort());
        if (bridgeChannel == null) {

            // 该端口还没有代理客户端
            ctx.channel().close();
        } else {
            Channel proxyChannel = outChannel.attr(Consts.NEXT_CHANNEL).get();
            if (proxyChannel != null) {
                proxyChannel.config().setOption(ChannelOption.AUTO_READ, outChannel.isWritable());
            }
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
