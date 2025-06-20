package com.example.datarouter.config.flow;

import com.example.datarouter.channel.DynamicChannelRegistry;
import com.example.datarouter.config.flow.model.Flow;
import com.example.datarouter.config.flow.model.FlowStep;
import com.example.datarouter.gateway.http.inbound.factory.HttpGatewayFactory;
import com.example.datarouter.router.dynamic.DynamicMessageRouter;
import com.example.datarouter.router.dynamic.factory.DynamicMessageRouterFactory;
import com.example.datarouter.service.factory.ServiceFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.stereotype.Component;

/**
 * FlowBuilder is responsible for constructing integration flows based on the provided FlowStep and Flow configurations.
 * It utilizes various factories to create routers, HTTP gateways, and service processors as needed.
 */
@Component
@RequiredArgsConstructor
public class FlowBuilder {

    private final DynamicChannelRegistry channelRegistry;

    private final DynamicMessageRouterFactory routerFactory;

    private final HttpGatewayFactory httpGatewayFactory;

    private final ServiceFactory serviceFactory;

    /**
     * Builds an IntegrationFlow based on the provided FlowStep and Flow configurations.
     * It sets up the flow with appropriate channels, logging, and processing steps.
     *
     * @param flowStep the FlowStep configuration containing details for building the flow
     * @param flow the Flow configuration that contains the overall flow structure
     * @return an IntegrationFlow object representing the constructed flow
     */
    public IntegrationFlow buildFlow(FlowStep flowStep, Flow flow) {
        var flowBuilder = IntegrationFlow.from(channelRegistry.getOrCreateChannel(flowStep.getInputChannel()));

        if (Boolean.TRUE.equals(flowStep.getEnableLogging())) {
            flowBuilder.log(LoggingHandler.Level.INFO, flowStep.getName() + " processing");
        }

        switch (flowStep.getType().toLowerCase()) {
            case "router":
                DynamicMessageRouter router = routerFactory.createRouter(flowStep);
                flowBuilder.route(router);
                break;
            case "httpinbound":
                httpGatewayFactory.createGateway(flowStep);
                break;
            case "service":
                var processor = serviceFactory.createOrGetProcessor(flowStep);
                flowBuilder.handle(processor);
                if (flowStep.getOutputChannel() != null) {
                    flowBuilder.channel(channelRegistry.getOrCreateChannel(flowStep.getOutputChannel()));
                }
                break;

            default:
                if (flowStep.getOutputChannel() != null) {
                    flowBuilder.channel(channelRegistry.getOrCreateChannel(flowStep.getOutputChannel()));
                }
        }

        return flowBuilder.get();
    }
}
