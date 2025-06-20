package com.example.datarouter.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DynamicChannelRegistry {

    private final Map<String, MessageChannel> channels = new ConcurrentHashMap<>();

    public MessageChannel getOrCreateChannel(String channelName) {
        return channels.computeIfAbsent(channelName, name -> {
            log.info("Creating channel [channel={}]", channelName);

            DirectChannel channel = new DirectChannel();
            channel.setBeanName(name);
            return channel;
        });
    }

    public MessageChannel getChannel(String channelName) {
        return channels.get(channelName);
    }

    public boolean exists(String channelName) {
        return channels.containsKey(channelName);
    }
}
