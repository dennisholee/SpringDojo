package io.forest.springai;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PipelineTest {

    @Autowired
    Pipeline pipeline;

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

        pipeline.run(chunkingStrategy);
    }
}