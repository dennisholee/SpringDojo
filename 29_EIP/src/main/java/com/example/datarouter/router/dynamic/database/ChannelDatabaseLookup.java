package com.example.datarouter.router.dynamic.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelDatabaseLookup implements ChannelLookup {

    private final RouteConfigRepository routeConfigRepository;

    @Override
    public String lookup(Message<?> message) {

        MessageHeaders headers = message.getHeaders();
        String businessProcess = headers.get("businessProcess", String.class);

        String channelName = routeConfigRepository.findByHeaderName(businessProcess);

        return channelName;
    }
}
