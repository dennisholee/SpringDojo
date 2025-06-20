package com.example.datarouter.enricher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "enricher.rest-client")
public class RestClientProperties {
    private String baseUrl = "https://api.example.com";
    private int connectTimeout = 5000;
    private int readTimeout = 5000;
    private String apiKey;
    private Map<String, String> defaultHeaders = new HashMap<>();
}
