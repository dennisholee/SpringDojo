package com.example.datarouter.config.flow.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Data
public class FlowStep {
    @NotBlank(message = "Step name is required")
    private String name;

    @NotBlank(message = "Step type is required")
    private String type;

    private String inputChannel;
    private String outputChannel;
    private String expression;
    private Boolean enableLogging;
    private String componentName;
    private String errorChannel;
    private Map<String, Object> properties;
}
