package io.forest.testcontext;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;


@Component
public class AddressAPIClient {

    @Value("${addressClient.api.basePath}")
    private String basePath;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplateBuilder().rootUri(basePath).build();
    }

    public Address findByPartyId(UUID partyId) {

        return this.restTemplate.getForObject(
            UriComponentsBuilder.fromPath("/address")
                .queryParam("partyId", partyId.toString())
                .toUriString(),
            Address.class);
    }
}
