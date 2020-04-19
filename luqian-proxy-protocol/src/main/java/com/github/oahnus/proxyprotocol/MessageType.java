package com.github.oahnus.proxyprotocol;

/**
 * Created by oahnus on 2020-03-31
 * 15:51.
 */
public class MessageType {
    public static final byte CONNECT = 0x01;
    public static final byte AUTH = 0x02;
    public static final byte HEARTBEAT = 0x03;
    public static final byte PROXY = 0x04;
    public static final byte DISCONNECT = 0x05;
    public static final byte ERROR = 0x06;
}
