package com.example.datarouter.gateway.http.inbound.factory;

import com.example.datarouter.channel.DynamicChannelRegistry;
import com.example.datarouter.config.flow.model.FlowStep;
import com.example.datarouter.gateway.http.inbound.DefaultHttpGateway;
import com.example.datarouter.gateway.http.inbound.registrar.HttpGatewayRegistrar;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpGatewayFactory {

    private final HttpGatewayRegistrar gatewayRegistrar;

    private final DynamicChannelRegistry channelRegistry;

    private final ConfigurableApplicationContext applicationContext;

    @PostConstruct
    public void init() {
//        RootBeanDefinition beanDefinition = new RootBeanDefinition(DefaultHttpGateway.class);
//        beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
//        beanDefinition.setLazyInit(true);
//
//        ((BeanDefinitionRegistry) applicationContext).registerBeanDefinition(
//            "defaultHttpGateway",
//            beanDefinition);
    }

    public DefaultHttpGateway createGateway(FlowStep step) {
        Map<String, Object> properties = step.getProperties();

        MessageChannel inputChannel = channelRegistry.getOrCreateChannel(step.getInputChannel());

        MessageChannel replyChannel = step.getOutputChannel() != null ?
            channelRegistry.getOrCreateChannel(step.getOutputChannel()) : null;

        RootBeanDefinition beanDefinition = new RootBeanDefinition(DefaultHttpGateway.class);

        ((BeanDefinitionRegistry) applicationContext).registerBeanDefinition(
            step.getName(),
            beanDefinition);

        DefaultHttpGateway gateway = (DefaultHttpGateway) applicationContext.getBean(
            step.getName(),
            inputChannel,
            replyChannel,
            getStringProperty(properties, "path", "/api/messages"),
            getStringProperty(properties, "method", "POST"),
            replyChannel != null
        );

        log.info("Creating HTTP gateway [path={}, method={}]", gateway.getPath(), gateway.getMethod());

        gatewayRegistrar.registerGateway(gateway);

        return gateway;
    }

    private String getStringProperty(Map<String, Object> properties, String key, String defaultValue) {
        return properties != null && properties.containsKey(key)
            ? properties.get(key).toString()
            : defaultValue;
    }
}
