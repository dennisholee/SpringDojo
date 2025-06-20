package com.example.datarouter.gateway.http.inbound.base;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.http.inbound.BaseHttpInboundEndpoint;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

public abstract class AbstractHttpGateway extends BaseHttpInboundEndpoint {

    public AbstractHttpGateway(MessageChannel requestChannel, MessageChannel replyChannel, boolean expectReply) {
        super(expectReply);

        super.setRequestChannel(requestChannel);
        super.setReplyChannel(replyChannel);

        setRequestPayloadType(ResolvableType.forClass(byte[].class));
        setHeaderMapper(new DefaultHttpHeaderMapper());
    }

    protected <T> Message<?> sendToChannel(Message<T> message) {
        org.springframework.messaging.Message<?> integrationMessage = MessageBuilder
            .withPayload(message.getPayload())
            .copyHeaders(message.getHeaders())
            .build();

        return super.sendAndReceiveMessage(integrationMessage);
    }

    protected abstract void validateMessage(Object message);
}
