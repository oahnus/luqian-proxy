package com.github.oahnus.proxyprotocol;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import javafx.util.converter.LongStringConverter;

import java.util.List;
import java.util.Map;


/**
 * Created by oahnus on 2020-04-01
 * 11:00.
 */
public class Consts {
    public static final AttributeKey<String> CHANNEL_ID = AttributeKey.newInstance("channel_id");
    public static final AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("next");

    public static final AttributeKey<String> TOKEN = AttributeKey.newInstance("token");
    public static final AttributeKey<String> APP_ID = AttributeKey.newInstance("app_id");
    public static final AttributeKey<Map<String, Channel>> OUTSIDE_CHANNELS = AttributeKey.newInstance("outside_channels");
    public static final AttributeKey<List<Integer>> OUTSIDE_PORTS = AttributeKey.newInstance("outside_ports");


    public static final int TYPE_LEN = 1;
    public static final int URI_LEN = 2;
    public static final int ID_LEN = 8;
    public static final int VERSION_LEN = 1;
}
