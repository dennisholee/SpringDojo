package com.example.datarouter.config.flow.reader.impl;

import com.example.datarouter.config.flow.model.FlowConfiguration;
import com.example.datarouter.config.flow.reader.ConfigurationReader;
import com.example.datarouter.config.flow.validator.FlowConfigurationValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.io.IOException;

/**
 * Implementation of ConfigurationReader that reads flow configurations from JSON files.
 * It uses Jackson ObjectMapper to deserialize JSON into FlowConfiguration objects.
 */

@Component
@RequiredArgsConstructor
public class JsonConfigurationReader implements ConfigurationReader {

    private final ObjectMapper objectMapper;

    private final ResourceLoader resourceLoader;

    private final FlowConfigurationValidator validator;

    /**
     * Reads a FlowConfiguration from a JSON file specified by the configPath.
     * @param configPath the path to the configuration file, which can be a classpath resource or a file system path.
     * @return the FlowConfiguration object parsed from the JSON file.
     * @throws IOException if there is an error reading the file or parsing the JSON.
     */
    @Override
    public FlowConfiguration read(String configPath) throws IOException {
        Resource resource = resourceLoader.getResource(configPath);
        return read(resource);
    }

    /**
     * Reads a FlowConfiguration from a Resource.
     * @param resource the Resource containing the configuration data.
     * @return the FlowConfiguration object parsed from the Resource.
     * @throws IOException if there is an error reading the Resource or parsing the JSON.
     */
    @Override
    public FlowConfiguration read(Resource resource) throws IOException {
        FlowConfiguration configuration = objectMapper.readValue(
            resource.getInputStream(),
            FlowConfiguration.class
        );

        validator.validate(configuration);
        return configuration;
    }

    /**
     * Checks if this reader supports the given configuration path.
     * It supports paths ending with ".json" or containing "/flows/".
     * @param configPath the configuration path to check.
     * @return true if the path is supported, false otherwise.
     */
    @Override
    public boolean supports(String configPath) {
        return configPath != null &&
               (configPath.endsWith(".json") || configPath.contains("/flows/"));
    }
}
