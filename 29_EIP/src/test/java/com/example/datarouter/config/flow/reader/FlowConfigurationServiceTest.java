package com.example.datarouter.config.flow.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FlowConfigurationServiceTest {

    @Mock
    List<ConfigurationReader> readers;

    @InjectMocks
    FlowConfigurationService flowConfigurationService;

    @Nested
    @DisplayName("")
    class LoadConfigurationTests {

        @DisplayName("should load configuration from file")
        void shouldLoadConfigurationFromFile() {
            String configPath = "classpath:flows/sample-flow.json";
            flowConfigurationService.loadConfiguration(configPath);
        }

        @DisplayName("should throw exception for unsupported file type")
        void shouldThrowExceptionForUnsupportedFileType() {
            // Implement test logic here
        }
    }
}