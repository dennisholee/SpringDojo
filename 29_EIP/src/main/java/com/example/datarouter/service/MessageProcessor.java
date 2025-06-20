package com.example.datarouter.service;


import org.springframework.messaging.Message;

public interface MessageProcessor {
    <T, R> Message<R> processMessage(Message<T> message);
}
