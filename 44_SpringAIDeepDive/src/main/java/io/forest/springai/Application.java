package io.forest.springai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Profile 	        chunkSize minChunkSizeChars	minChunkLengthToEmbed	maxNumChunks	Expected Behavior
     * The Specialist   256	      50	            100                     20	            High Precision: Captures granular facts/FAQs. Prevents "dilution" by keeping segments tight and specific.
     * The Generalist   512	      150               200                     5	            Balanced: Best for wikis. Filters out "noise" (headers/footers) while keeping enough narrative context.
     * The Researcher   1024      300	            500	                    3               Deep Context: Best for legal/long-form docs. High quality gate ensures only significant blocks are indexed.
     * The Rejector .   256 .     800 .            1000                     5               Exception Case: min is higher than chunkSize. Expect zero documents to be indexed (Logical Failure test).
     * @return
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
