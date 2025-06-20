package com.example.datarouter.config.flow;

import com.example.datarouter.channel.DynamicChannelRegistry;
import com.example.datarouter.config.flow.model.Flow;
import com.example.datarouter.config.flow.model.FlowConfiguration;
import com.example.datarouter.config.flow.reader.FlowConfigurationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowInitializer {

    private final FlowConfigurationService configService;
    private final DynamicChannelRegistry channelRegistry;
    private final DynamicFlowRegistry flowRegistry;
    private final ResourcePatternResolver resourcePatternResolver;

    @PostConstruct
    public void initialize() {
        try {
            // Scan for all JSON files in the flows directory
            Resource[] flowResources = resourcePatternResolver.getResources("classpath:flows/*.json");

            log.info("Found {} flow configuration files", flowResources.length);

            Arrays.stream(flowResources)
                .forEach(this::initializeFlowFromResource);

        } catch (IOException e) {
            log.error("Failed to scan flow configuration directory", e);
            throw new RuntimeException("Failed to initialize flows", e);
        }
    }

    void initializeFlowFromResource(Resource resource) {
        try {
            log.info("Initializing flow from: {}", resource.getFilename());

            FlowConfiguration config = configService.loadConfiguration(resource);
            initializeFlow(config.getFlow());

            log.info("Successfully initialized flow: {}", config.getMetadata().getName());
        } catch (Exception e) {
            log.error("Failed to initialize flow from: " + resource.getFilename(), e);
            throw new RuntimeException("Flow initialization failed", e);
        }
    }

    void initializeFlow(Flow flow) {
        // Create all channels first
        flow.getSteps().forEach(step -> {
            Stream.of(
                    step.getInputChannel(),
                    step.getOutputChannel(),
                    step.getErrorChannel())
                .filter(Objects::nonNull)
                .forEach(channelRegistry::getOrCreateChannel);
        });

        // Register all flows
        flowRegistry.registerFlow(flow);
    }
}
