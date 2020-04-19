package com.github.oahnus.proxyclient.inter;

import io.netty.channel.Channel;

/**
 * Created by oahnus on 2020-04-09
 * 11:16.
 */
public interface CacheStatusListener {
    void success(Channel channel);
    void fail();
}
