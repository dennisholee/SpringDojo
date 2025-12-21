package io.forest.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

/**
 * Top-level configuration for running the demo RAG pipeline.
 *
 * <p>Provides a RestClient configured to use HTTP/1.1 (helpful for compatibility
 * with LM Studio or local model servers) and a CommandLineRunner that triggers
 * the {@link IngestionPipeline} on startup.
 */
@Configuration
public class RagPipelineConfig {

    /**
     * Create a RestClient.Builder that forces HTTP/1.1. Some local LLM servers
     * (including LM Studio) may have issues with HTTP/2; forcing HTTP/1.1 avoids
     * those networking issues during local testing.
     *
     * @return a RestClient.Builder backed by the JDK HTTP client set to HTTP/1.1
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }

    /**
     * CommandLineRunner that executes the ingestion pipeline on startup and
     * performs a sample similarity search to demonstrate embeddings were stored.
     *
     * @param pipeline the IngestionPipeline used to fetch and process the URL
     * @param vectorStore the VectorStore used to query for similar documents
     * @return a CommandLineRunner that triggers the demo flow
     */
    @Bean
    public CommandLineRunner runner(IngestionPipeline pipeline, VectorStore vectorStore) {
        return args -> {
            // Run the pipeline for a well-known example page
            pipeline.runPipeline("https://info.cern.ch/hypertext/WWW/TheProject.html");
            System.out.println("Pipeline execution complete.");

            // Verify results with a simple similarity query
            List<Document> results = vectorStore.similaritySearch("protocols");
            results.forEach(doc -> {
                System.out.println("Content: " + doc.getText());
                System.out.println("Metadata: " + doc.getMetadata());
            });
        };
    }
}
