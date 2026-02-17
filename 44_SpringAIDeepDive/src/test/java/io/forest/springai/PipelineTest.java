package io.forest.springai;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration-style test that runs the demo pipeline with multiple
 * chunking strategies. The test is intentionally lightweight and primarily
 * documents expected inputs for developers exploring the project.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PipelineTest {

    @Autowired
    Pipeline pipeline;

    // String query = "What are the key features of Spring AI and how does it simplify Java AI development?";

    // String query = "How does Spring AI use Spring Boot to help Java engineers build RAG applications?";

    //String query = "How does Spring AI interact with external APIs?";

    // String query = "Does Spring AI handle service discovery?";

    // String query = "Explain the modular principles of Spring AI.";

    @ParameterizedTest()
    @CsvSource({
        "The Specialist, 256, 50, 100, 20",
        "The Generalist, 512, 150, 200, 5",
        "The Researcher, 1024, 300, 500, 3",
        "The Rejector, 256, 800, 1000, 5",
        "The Fragmenter, 32, 10, 20, 50"
    })
    void test(String name,
              int chunkSize,
              int minChunkSizeChars,
              int minChunkLengthToEmbed,
              int maxNumChunks) {

        ChunkingStrategy chunkingStrategy = new ChunkingStrategy(
            name,
            chunkSize,
            minChunkSizeChars,
            minChunkLengthToEmbed,
            maxNumChunks);

        String query = "How do I use Virtual Threads in Spring AI?";

        pipeline.run(query, chunkingStrategy);
    }
}