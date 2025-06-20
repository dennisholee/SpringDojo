package com.example.datarouter.gateway.http.inbound.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JacksonMessageBinder {

    private final ObjectMapper objectMapper;

    public Message<?> readMessage(byte[] body) throws IOException {
        return objectMapper.readValue(body, Message.class);
    }

    public byte[] writeMessage(Message<?> message) throws IOException {
        return objectMapper.writeValueAsBytes(message);
    }
}
