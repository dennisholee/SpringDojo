package com.example.datarouter.config.flow.validator;

import com.example.datarouter.config.flow.model.FlowConfiguration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for FlowConfiguration objects.
 * It uses Jakarta Bean Validation to ensure that the configuration adheres to the defined constraints.
 */
@Component
@RequiredArgsConstructor
public class FlowConfigurationValidator {

    private final Validator validator;

    /**
     * Validates the given FlowConfiguration object.
     * If there are any constraint violations, it throws a FlowConfigurationException with detailed error messages.
     *
     * @param configuration the FlowConfiguration object to validate
     * @throws FlowConfigurationException if validation fails
     */
    public void validate(FlowConfiguration configuration) {

        Set<ConstraintViolation<FlowConfiguration>> violations = validator.validate(configuration);

        if (!violations.isEmpty()) {
            String errorMessages = violations.stream()
                .map(violation -> "%s: %s".formatted(violation.getPropertyPath(), violation.getMessage()))
                .collect(Collectors.joining(", "));

            throw new FlowConfigurationException("Configuration validation failed: " + errorMessages);
        }
    }
}
