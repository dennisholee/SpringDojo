package com.example.datarouter.router.dynamic;

import com.example.datarouter.router.dynamic.database.ChannelLookup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

import java.util.Collection;
import java.util.Collections;

@Slf4j
public class DynamicMessageRouter extends AbstractMessageRouter {

    private final ChannelLookup channelLookup;

    public DynamicMessageRouter(ChannelLookup channelLookup,
                              DestinationResolver<MessageChannel> channelResolver) {
        this.channelLookup = channelLookup;
        setChannelResolver(channelResolver);
    }

    @Override
    protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
        String channelName = channelLookup.lookup(message);

        log.info("Routing message to channel: {}", channelName);

        if (channelName == null) {
            channelName = "defaultChannel";
        }

        MessageChannel channel = getChannelResolver().resolveDestination(channelName);
        return Collections.singletonList(channel);
    }
}
