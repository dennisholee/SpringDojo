package com.example.datarouter.config.flow.validator;

/**
 * Exception thrown when there is an error in flow configuration validation.
 */
public class FlowConfigurationException extends RuntimeException {

    /**
     * Constructs a new FlowConfigurationException with the specified detail message.
     *
     * @param message the detail message
     */
    public FlowConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new FlowConfigurationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public FlowConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
