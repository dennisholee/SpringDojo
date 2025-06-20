package com.example.datarouter.config.flow.reader.impl;

import com.example.datarouter.config.flow.model.FlowConfiguration;
import com.example.datarouter.config.flow.validator.FlowConfigurationValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonConfigurationReaderTest {

    @Mock
    ResourceLoader resourceLoader;

    @Mock
    FlowConfigurationValidator flowConfigurationValidator;

    @Spy
    ObjectMapper objectMapper;

    @InjectMocks
    JsonConfigurationReader jsonConfigurationReader;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("")
    class ReadConfigPathTests {

        @Test
        @DisplayName("should read configuration from valid JSON file")
        void shouldReadConfigurationFromValidJsonFile() throws IOException {
            String configPath = "classpath:flows/sample-flow.json";

            when(resourceLoader.getResource(eq(configPath))).thenReturn(new ClassPathResource("flows/sample-flow.json"));

            FlowConfiguration configuration = jsonConfigurationReader.read(configPath);

            assertAll(
                () -> assertNotNull(configuration, "Configuration should not be null"),
                () -> assertThat(configuration, allOf(
                    hasProperty("version", equalTo("1.0")),
                    hasProperty("metadata", allOf(
                        hasProperty("name", equalTo("Integration Flow Configuration")),
                        hasProperty("description", equalTo("Defines all integration flows for the data router"))
                    ))
                ))
            );
        }

        @DisplayName("should throw exception for invalid JSON format")
        void shouldThrowExceptionForInvalidJsonFormat() {
            // Implement test logic here
        }

        @DisplayName("should validate configuration after reading")
        void shouldValidateConfigurationAfterReading() {
            // Implement test logic here
        }
    }
}