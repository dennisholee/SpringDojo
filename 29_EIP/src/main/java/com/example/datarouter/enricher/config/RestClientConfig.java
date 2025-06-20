package com.example.datarouter.enricher.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final RestClientProperties properties;

    @Bean
    public RestTemplate enricherRestTemplate() {
        return new RestTemplateBuilder()
            .connectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
            .readTimeout(Duration.ofMillis(properties.getReadTimeout()))
            .defaultHeader("X-API-Key", properties.getApiKey())
            .build();
    }
}
