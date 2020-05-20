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
}
