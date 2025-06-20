package com.example.datarouter.enricher;

import org.springframework.integration.core.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

public abstract class AbstractContentEnricher<T> implements ContentEnricher<T> {

    @Override
    public Message<T> enrich(Message<T> message) {

//        Map<String, Object> enrichedHeaders = enrichHeaders(
//            message.getHeaders() != null ? message.getHeaders() : new HashMap<>()
//        );

        T enrichedPayload = enrichPayload(message.getPayload());

        return MessageBuilder.createMessage(enrichedPayload, message.getHeaders());
    }

    @Override
    public Map<String, Object> enrichHeaders(Map<String, Object> headers) {
        return headers;
    }

    protected abstract T enrichPayload(T payload);
}
