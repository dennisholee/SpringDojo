package com.example.datarouter.service.base;

import com.example.datarouter.service.MessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

public abstract class AbstractMessageProcessor implements MessageProcessor {

    public <T,R> Message<R> processMessage(Message<T> message) {
        beforeProcess(message.getPayload(), message.getHeaders());
        R result = doProcess(message.getPayload(), message.getHeaders());
        afterProcess(result, message.getHeaders());

        return MessageBuilder.createMessage(result, message.getHeaders());
    }

    protected <T> void beforeProcess(T message, MessageHeaders headers) {
        // Hook for pre-processing
    }

    protected  abstract <T,R> R doProcess(T message, MessageHeaders headers);

    protected <R> void afterProcess(R result, MessageHeaders headers) {
        // Hook for post-processing
    }
}
