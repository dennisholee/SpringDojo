package com.example.datarouter.config.flow.model;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Data
public class FlowConfiguration {
    @NotNull(message = "Version is required")
    private String version;

    @Valid
    @NotNull(message = "Metadata is required")
    private FlowMetadata metadata;

    @Valid
    @NotNull(message = "Flow configuration is required")
    private Flow flow;
}
