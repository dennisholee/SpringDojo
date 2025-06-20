package com.example.datarouter.config.flow.reader;

import com.example.datarouter.config.flow.model.FlowConfiguration;
import com.example.datarouter.config.flow.validator.FlowConfigurationException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.List;

/**
 * Service for loading flow configurations from various sources.
 * It uses a list of ConfigurationReader implementations to read configurations
 * based on the file path or resource provided.
 */
@Service
@RequiredArgsConstructor
public class FlowConfigurationService {

    private final List<ConfigurationReader> readers;

    /**
     * Loads a FlowConfiguration from a file specified by the configPath.
     * The configPath can be a classpath resource or a file system path.
     *
     * @param configPath the path to the configuration file
     * @return the FlowConfiguration object parsed from the file
     * @throws FlowConfigurationException if no suitable reader is found or if there is an error reading the configuration
     */
    public FlowConfiguration loadConfiguration(String configPath) {
        ConfigurationReader reader = findSuitableReader(configPath);
        try {
            return reader.read(configPath);
        } catch (IOException e) {
            throw new FlowConfigurationException("Failed to read configuration: " + configPath, e);
        }
    }

    /**
     * Loads a FlowConfiguration from a Resource.
     *
     * @param resource the Resource containing the configuration data
     * @return the FlowConfiguration object parsed from the Resource
     * @throws IOException if there is an error reading the Resource or parsing the configuration
     */
    public FlowConfiguration loadConfiguration(Resource resource) throws IOException {
        String path = resource.getURI().toString();
        ConfigurationReader reader = findSuitableReader(path);
        return reader.read(resource);
    }

    /**
     * Finds a suitable ConfigurationReader for the given path.
     * It checks each registered reader to see if it supports the specified path.
     *
     * @param path the configuration path to check
     * @return a ConfigurationReader that supports the specified path
     * @throws FlowConfigurationException if no suitable reader is found
     */
    private ConfigurationReader findSuitableReader(String path) {
        return readers.stream()
            .filter(r -> r.supports(path))
            .findFirst()
            .orElseThrow(() -> new FlowConfigurationException(
                "No suitable configuration reader found for: " + path));
    }
}
