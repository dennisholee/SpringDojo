package io.forest.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Pipeline {

    final ChatClient.Builder builder;

    final ChromaApi chromaApi;

    final VectorStore vectorStore;

    final List<ChunkingStrategy> chunkingStrategies;

    final RelevancyEvaluator relevancyEvaluator;

    public Pipeline(ChatClient.Builder builder, List<ChunkingStrategy> chunkingStrategies, ChromaApi chromaApi, VectorStore vectorStore) {
        this.builder = builder;
        this.chromaApi = chromaApi;
        this.vectorStore = vectorStore;
        this.chunkingStrategies = chunkingStrategies;
        this.relevancyEvaluator = new RelevancyEvaluator(builder);
    }

    public void run(ChunkingStrategy strategy) {

        TextReader textReader = new TextReader("springai.txt");

        //for (ChunkingStrategy strategy : chunkingStrategies) {

        System.out.println("=".repeat(80));
        System.out.println(strategy.name());

        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
            .withChunkSize(strategy.chunkSize())
            .withMinChunkSizeChars(strategy.minChunkSizeChars())
            .withMinChunkLengthToEmbed(strategy.minChunkLengthToEmbed())
            .withMaxNumChunks(strategy.maxNumChunks())
            .build();

        vectorStore.accept(
            textSplitter.apply(
                textReader.get()));

        // String query = "What are the key features of Spring AI and how does it simplify Java AI development?";

        String query = "How does Spring AI use Spring Boot to help Java engineers build RAG applications?";
        SearchRequest searchRequest = SearchRequest.builder()
            .query(query) // Used for the Vector Search
            .topK(5)            // Number of records to see
            .build();

        List<Document> similaritySearch = vectorStore.similaritySearch(searchRequest);

//            similaritySearch.stream()
//                .forEach(d -> System.out.println(d.getText()));

        similaritySearch.stream()
            .forEach(d -> System.out.printf(
                "chunk_index=%d, distance=%f\n",
                d.getMetadata().get("chunk_index"),
                d.getMetadata().get("distance")
            ));

        ChatResponse chatResponse = this.builder.build()
            .prompt()
            .user(u -> u.text(query))
            .advisors(QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build())
            .call()
            .chatResponse();

        EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(
            new EvaluationRequest(
                query, // Used for the LLM Instruction
                similaritySearch,
                chatResponse.getResult().getOutput().getText()));

//            new ChunkingMetrics(
//                evaluationResponse.
//            );

        if (evaluationResponse.isPass()) {
            System.out.println("Chunking Strategy Effective (Score: " + evaluationResponse.getScore() + ")");
        } else {
            System.out.println("Chunking Strategy Failed: Response was not relevant to retrieved chunks.");
        }

        System.out.println("similaritySearch> " + similaritySearch.getFirst().getText());
        System.out.println("chatResponse    > " + chatResponse.getResult().getOutput().getText());
        //}

        this.chromaApi.deleteCollection("default_tenant", "default_database", "my_collection");


    }
}
