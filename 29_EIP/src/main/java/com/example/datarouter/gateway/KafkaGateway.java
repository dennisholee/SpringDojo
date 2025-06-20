package com.example.datarouter.gateway;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;

@MessagingGateway(defaultRequestChannel = "kafkaInboundChannel")
public interface KafkaGateway {

    @Gateway
    void processMessage(Message<?> message);
}
