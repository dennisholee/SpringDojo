package com.example.datarouter.service.impl;

import com.example.datarouter.service.base.AbstractMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
public class LoggingMessageProcessor extends AbstractMessageProcessor {

    @Override
    protected <T, R> R doProcess(T message, MessageHeaders headers) {

        log.info("Processing message: {}", message);

        String processedPayload = "Processed: " + message;

        log.info("Processed payload: {}", processedPayload);

        return (R) processedPayload;
    }

    protected void beforeProcess(String message, MessageHeaders headers) {
        System.out.println("Processing message with headers: " + headers);
    }

    protected void afterProcess(String result, MessageHeaders headers) {
        System.out.println("Finished processing message");
    }
}
