package com.example.datarouter.config.flow.reader;

import com.example.datarouter.config.flow.model.FlowConfiguration;
import org.springframework.core.io.Resource;
import java.io.IOException;

/**
 * Interface for reading flow configurations from various sources.
 * Implementations should provide methods to read configurations from file paths or resources.
 */
public interface ConfigurationReader {

    /**
     * Reads a FlowConfiguration from a file specified by the configPath.
     * @param configPath the path to the configuration file, which can be a classpath resource or a file system path.
     * @return the FlowConfiguration object parsed from the file.
     * @throws IOException if there is an error reading the file or parsing the configuration.
     */
    FlowConfiguration read(String configPath) throws IOException;

    /**
     * Reads a FlowConfiguration from a Resource.
     * @param resource the Resource containing the configuration data.
     * @return the FlowConfiguration object parsed from the Resource.
     * @throws IOException if there is an error reading the Resource or parsing the configuration.
     */
    FlowConfiguration read(Resource resource) throws IOException;

    /**
     * Checks if this reader supports the given configuration path.
     * @param configPath the configuration path to check.
     * @return true if this reader can handle the specified path, false otherwise.
     */
    boolean supports(String configPath);
}
