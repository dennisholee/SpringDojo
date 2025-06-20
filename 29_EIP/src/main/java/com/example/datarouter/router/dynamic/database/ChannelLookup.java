package com.example.datarouter.router.dynamic.database;

import org.springframework.messaging.Message;

public interface ChannelLookup {
    String lookup(Message<?> message);
}
