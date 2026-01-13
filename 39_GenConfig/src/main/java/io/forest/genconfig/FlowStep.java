package io.forest.genconfig;

import java.util.List;
import java.util.Map;

public record FlowStep(
    String type,        // e.g., "splitter", "aggregator"
    String store,
    String bean,
    String channel,
    String requestChannel,
    String expression,   // Any SpEL or logic captured
    int maxAttempts,
    int backOff,
    String manager,
    int timeout,
    int groupTimeout,
    boolean sendPartialResultsOnExpiry,
    String method,
    String correlationStrategy,
    String releaseStrategy,
    String defaultOutputChannel,
    int poolSize,
    int queueCapacity,
    String url,
    List<Recipient> recipients,
    Map<String, String> propertyMappings,
    List<String> channels,
    Map<String, String> mapping

) {}
