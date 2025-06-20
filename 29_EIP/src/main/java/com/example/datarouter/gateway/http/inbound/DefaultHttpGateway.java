package com.example.datarouter.gateway.http.inbound;

import com.example.datarouter.gateway.http.inbound.base.AbstractHttpGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.RequestBody;

public class DefaultHttpGateway extends AbstractHttpGateway {

    private final String path;
    private final String method;

    public DefaultHttpGateway(MessageChannel requestChannel,
                              MessageChannel replyChannel,
                              String path,
                              String method,
                              boolean expectReply) {
        super(requestChannel, replyChannel, expectReply);

        this.path = path;
        this.method = method;
    }

    public ResponseEntity<Message> handleRequest(@RequestBody Object message) {
        validateMessage(message);

        Message msg = MessageBuilder.withPayload(message).build();

        Message response = sendToChannel(msg);

        return ResponseEntity.ok(response);
    }

    @Override
    protected void validateMessage(Object message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }
}
