package com.github.oahnus.proxyprotocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by oahnus on 2020-03-31
 * 15:42.
 */
public class ProxyProtocolEncoder extends MessageToByteEncoder<NetMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, NetMessage msg, ByteBuf out) throws Exception {
        byte[] dataBytes = msg.getData();
        String uri = msg.getUri();
        String id = msg.getId();
        String version = msg.getVersion();

        int dataLen = 0;
        if (dataBytes != null) {
            dataLen = dataBytes.length;
        }
        int uriLen = 0;
        if (uri != null && !uri.equals("")) {
            uriLen = uri.getBytes().length;
        }
        int idLen = id.getBytes().length;
        int versionLen = version.getBytes().length;
        int frameLen = Consts.TYPE_LEN + idLen + 4 + uriLen + 4 + versionLen + 4 + dataLen + 4;

        // [frameLen][msgType][idLen][id][uriLen][uri][versionLen][version][dataLen][data]
        out.writeInt(frameLen);
        out.writeByte(msg.getType());
        out.writeInt(idLen).writeBytes(id.getBytes());
        out.writeInt(uriLen);
        if (uriLen != 0) {
            out.writeBytes(uri.getBytes());
        }
        out.writeInt(versionLen);
        out.writeBytes(version.getBytes());
        out.writeInt(dataLen);
        if (dataLen > 0) {
            out.writeBytes(dataBytes);
        }
    }
}
