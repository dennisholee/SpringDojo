package com.example.datarouter.config.flow.model;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class Flow {
    @NotNull(message = "Flow name is required")
    private String name;

    private String description;

    @NotEmpty(message = "Flow must contain at least one step")
    @Valid
    private List<FlowStep> steps;
}
