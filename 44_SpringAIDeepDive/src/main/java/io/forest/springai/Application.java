package io.forest.springai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Main Spring Boot application for the Spring AI Deep Dive demo.
 *
 * <p>This class provides the application entry point and a couple of simple
 * Spring beans used by the demo. The embedded table below documents a set of
 * example chunking profiles used by the pipeline tests. The table is for
 * developer reference only and is not parsed at runtime.</p>
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }

    /**
     * Default Jackson {@link ObjectMapper} bean used throughout the demo to
     * serialize/deserialize JSON when needed.
     *
     * @return a fresh {@link ObjectMapper}
     */
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Provides a set of pre-configured {@link ChunkingStrategy} profiles used
     * by the integration-style pipeline tests. Each profile controls how text
     * is split into chunks before embeddings / vector indexing.
     *
     * <p>Columns: name, chunkSize, minChunkSizeChars, minChunkLengthToEmbed,
     * maxNumChunks — examples chosen to exercise different behaviors:</p>
     *
     * <ul>
     *   <li>The Specialist — tight chunks for high precision</li>
     *   <li>The Generalist — balanced chunks for typical documentation</li>
     *   <li>The Researcher — large chunks for long-form documents</li>
     *   <li>The Rejector — intentionally invalid (min &gt; chunkSize) to test rejection</li>
     *   <li>The Fragmenter — very small chunks to test fragmentation behavior</li>
     * </ul>
     *
     * @return list of example {@link ChunkingStrategy} profiles
     */
    @Bean
    List<ChunkingStrategy> chunkingStrategies() {
        return List.of(
            new ChunkingStrategy("The Specialist", 256, 50, 100, 20),
            new ChunkingStrategy("The Generalist", 512, 150, 200, 5),
            new ChunkingStrategy("The Researcher", 1024, 300, 500, 3),
            new ChunkingStrategy("The Rejector", 256, 800, 1000, 5),
            new ChunkingStrategy("The Fragmenter", 32, 10, 20, 50)

        );
    }
}
