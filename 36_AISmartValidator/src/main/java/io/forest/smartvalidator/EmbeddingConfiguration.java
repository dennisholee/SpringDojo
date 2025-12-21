package io.forest.smartvalidator;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.util.List;

@Configuration
public class EmbeddingConfiguration {

    /**
     * Define the custom RestClient here to force HTTP/1.1 for LM Studio.
     * This breaks the cycle because it's no longer in the class
     * that implements CommandLineRunner.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        return RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }

    @Bean
    public CommandLineRunner embeddingRunner(EmbeddingModel embeddingModel) {
        return args -> {
            System.out.println("--- Starting LM Studio Embedding Test ---");
            try {
                String text = "search_document: Hello world, testing Spring AI with LM Studio.";
                EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

                if (!response.getResults().isEmpty()) {
                    float[] vector = response.getResults().getFirst().getOutput();
                    System.out.println("Vector length: " + vector.length);
                }
            } catch (Exception e) {
                System.err.println("Embedding failed: " + e.getMessage());
            }
        };
    }
}