package com.github.oahnus.proxyprotocol;

import lombok.Data;

import java.util.UUID;

/**
 * Created by oahnus on 2020-03-31
 * 15:42.
 */
@Data
public class NetMessage {
    private String id;
    private String uri;
    private String version;
    private byte[] data;
    private byte type;

    public NetMessage() {
        this.id = UUID.randomUUID().toString();
        this.type = MessageType.HEARTBEAT;
        this.version = ProtocolVersion.VERSION_0_1;
    }

    public NetMessage(byte type) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.version = ProtocolVersion.VERSION_0_1;
    }

    public static NetMessage heartbeat() {
        return new NetMessage(MessageType.HEARTBEAT);
    }

    public static NetMessage notify(String msg) {
        NetMessage netMessage = new NetMessage(MessageType.INFO);
        netMessage.setData(msg.getBytes());
        return netMessage;
    }

    public static NetMessage error(String msg) {
        NetMessage netMessage = new NetMessage(MessageType.ERROR);
        netMessage.setData(msg.getBytes());
        return netMessage;
    }

    public static NetMessage proxy(String appId, String channelId, byte[] payloads) {
        NetMessage netMessage = new NetMessage(MessageType.PROXY);
        netMessage.setUri(appId + "#" + channelId);
        netMessage.setData(payloads);
        return netMessage;
    }

    public static NetMessage connect(String appId, String channelId, byte[] payloads) {
        NetMessage netMessage = new NetMessage(MessageType.CONNECT);
        netMessage.setUri(appId + "#" + channelId);
        netMessage.setData(payloads);
        return netMessage;
    }
    public static NetMessage disconnect(String appId, String channelId, byte[] payloads) {
        NetMessage netMessage = new NetMessage(MessageType.DISCONNECT);
        netMessage.setUri(appId + "#" + channelId);
        netMessage.setData(payloads);
        return netMessage;
    }

    public static NetMessage auth(String appId, String appSecret, byte[] payloads) {
        NetMessage netMessage = new NetMessage(MessageType.AUTH);
        netMessage.setUri(appId + "#" + appSecret);
        netMessage.setData(payloads);
        return netMessage;
    }
}
