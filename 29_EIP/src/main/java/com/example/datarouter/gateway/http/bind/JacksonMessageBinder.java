package com.example.datarouter.gateway.http.bind;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.integration.http.inbound.BaseHttpInboundEndpoint;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class JacksonMessageBinder {

    private final ObjectMapper objectMapper;
//
//    public JacksonMessageBinder() {
//        this.objectMapper = new ObjectMapper();
//    }
//
//    public JacksonMessageBinder(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//    }

    /**
     * Converts a Spring Integration Message to JSON bytes for HTTP response
     */
    public byte[] toJson(Message<?> message) throws IOException {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("payload", message.getPayload());
        messageMap.put("headers", message.getHeaders());
        return objectMapper.writeValueAsString(messageMap).getBytes();
    }

    /**
     * Converts JSON bytes from HTTP request to a Spring Integration Message
     */
    @SuppressWarnings("unchecked")
    public Message<?> fromJson(byte[] jsonBytes) throws IOException {
        Map<String, Object> messageMap = objectMapper.readValue(jsonBytes, Map.class);
        Object payload = messageMap.get("payload");
        Map<String, Object> headers = (Map<String, Object>) messageMap.get("headers");

        return MessageBuilder
            .withPayload(payload)
            .copyHeaders(headers)
            .build();
    }
}
