package com.example.datarouter.router.dynamic.factory;

import com.example.datarouter.channel.DynamicChannelRegistry;
import com.example.datarouter.config.flow.model.FlowStep;
import com.example.datarouter.gateway.http.inbound.DefaultHttpGateway;
import com.example.datarouter.router.dynamic.DynamicMessageRouter;
import com.example.datarouter.router.dynamic.database.ChannelLookup;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DynamicMessageRouterFactory {

    private final ChannelLookup channelLookup;

    private final DynamicChannelRegistry channelRegistry;

    private final ConfigurableApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(DynamicMessageRouter.class);
        beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);

        ((BeanDefinitionRegistry) applicationContext).registerBeanDefinition(
            "dynamicMessageRouter",
            beanDefinition);
    }

    public DynamicMessageRouter createRouter(FlowStep step) {
        DestinationResolver<MessageChannel> resolver = channelName ->
            channelRegistry.getOrCreateChannel(channelName);

        DynamicMessageRouter router = applicationContext.getBean(
            DynamicMessageRouter.class,
            channelLookup,
            resolver
        );
        // DynamicMessageRouter router = new DynamicMessageRouter(channelLookup, resolver);

        // Get the input channel and bind the router
        MessageChannel inputChannel = channelRegistry.getOrCreateChannel(step.getInputChannel());
        if (inputChannel instanceof DirectChannel) {
            ((DirectChannel) inputChannel).subscribe(router);
        }

        return router;
    }
}
