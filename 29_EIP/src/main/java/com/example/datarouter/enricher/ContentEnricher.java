package com.example.datarouter.enricher;


import org.springframework.messaging.Message;

import java.util.Map;

public interface ContentEnricher<T> {
    Message<T> enrich(Message<T> message);
    Map<String, Object> enrichHeaders(Map<String, Object> headers);
}
