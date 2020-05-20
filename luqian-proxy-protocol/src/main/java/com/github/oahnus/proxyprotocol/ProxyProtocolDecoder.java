package com.github.oahnus.proxyprotocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.ByteOrder;

/**
 * Created by oahnus on 2020-03-31
 * 15:38.
 */
public class ProxyProtocolDecoder extends LengthFieldBasedFrameDecoder {
    public ProxyProtocolDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }

    public ProxyProtocolDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    public ProxyProtocolDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    public ProxyProtocolDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        super(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    @Override
    protected NetMessage decode(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        ByteBuf in = (ByteBuf) super.decode(ctx, buf);

        if (in == null) {
            return null;
        }

        if (in.readableBytes() < 4) {
            return null;
        }

        // [frameLen][msgType][idLen][id][uriLen][uri][versionLen][version][dataLen][data]
        int frameLen = in.readInt();
        if (in.readableBytes() < frameLen) {
            return null;
        }

        byte msgType = in.readByte();
        int idLen = in.readInt();
        byte[] idBytes = new byte[idLen];
        in.readBytes(idBytes);
        String id = new String(idBytes);

        int uriLen = in.readInt();
        String uri = "";
        if (uriLen > 0) {
            byte[] uriBytes = new byte[uriLen];
            in.readBytes(uriBytes);
            uri = new String(uriBytes);
        }

        int versionLen = in.readInt();
        byte[] versionBytes = new byte[versionLen];
        in.readBytes(versionBytes);
        String version = new String(versionBytes);

        int dataLen = in.readInt();
        byte[] dataBytes = null;
        if (dataLen > 0){
            dataBytes = new byte[dataLen];
            in.readBytes(dataBytes);
        }
        in.release();

        NetMessage msg = new NetMessage();
        msg.setType(msgType);
        msg.setUri(uri);
        msg.setVersion(version);
        msg.setId(id);
        msg.setData(dataBytes);
        return msg;
    }
}
