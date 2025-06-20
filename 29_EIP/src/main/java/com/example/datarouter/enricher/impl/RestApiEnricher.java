package com.example.datarouter.enricher.impl;

import com.example.datarouter.enricher.AbstractContentEnricher;
import com.example.datarouter.enricher.config.RestClientProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestApiEnricher extends AbstractContentEnricher<String> {

    private final RestClientProperties properties;
    private final RestTemplate restTemplate;

    @Override
    protected String enrichPayload(String payload) {
        HttpHeaders headers = new HttpHeaders();
        properties.getDefaultHeaders().forEach(headers::add);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        return restTemplate.exchange(
            properties.getBaseUrl() + "/enrich",
            HttpMethod.POST,
            entity,
            String.class
        ).getBody();
    }

    @Override
    public Map<String, Object> enrichHeaders(Map<String, Object> headers) {
        headers.put("enriched", true);
        headers.put("enrichmentTimestamp", System.currentTimeMillis());
        return headers;
    }
}
