package io.forest.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The main demo pipeline that exercises document chunking, vector indexing,
 * semantic search, LLM-based evaluation, and metric computation.
 *
 * <p>This component is intentionally written as an integration-style demo
 * rather than a production-ready pipeline. It demonstrates how to wire
 * Spring AI components together: readers, splitters, vector stores, and
 * evaluators.</p>
 */
@Component
public class Pipeline {

    final ChatClient.Builder builder;

    final ChromaApi chromaApi;

    final VectorStore vectorStore;

    final Evaluator relevancyEvaluator;

    /**
     * Create the pipeline with the required components. A {@link ChatClient.Builder}
     * is used to construct the chat client for evaluation and enrichers.
     */
    public Pipeline(ChatClient.Builder builder, ChromaApi chromaApi, VectorStore vectorStore) {
        this.builder = builder;
        this.chromaApi = chromaApi;
        this.vectorStore = vectorStore;
        this.relevancyEvaluator = new DecimalRelevancyEvaluator(builder);
    }

    /**
     * Run the demo pipeline for a single query using the provided
     * {@link ChunkingStrategy}.
     *
     * Steps:
     * 1. Read the demo text file
     * 2. Split into chunks using the token splitter configured by the strategy
     * 3. Add chunks to the vector store
     * 4. Perform a semantic search and ask an LLM advisor for an answer
     * 5. Evaluate retrieved chunks with the LLM-based evaluator
     * 6. Compute and print aggregated metrics
     *
     * @param query the user query
     * @param strategy chunking configuration to use for splitting
     */
    public void run(String query, ChunkingStrategy strategy) {

        TextReader textReader = new TextReader("springai.txt");

        System.out.println("=".repeat(80));
        System.out.println(strategy.name());

        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
            .withChunkSize(strategy.chunkSize())
            .withMinChunkSizeChars(strategy.minChunkSizeChars())
            .withMinChunkLengthToEmbed(strategy.minChunkLengthToEmbed())
            .withMaxNumChunks(strategy.maxNumChunks())
            .build();

        // Split and index documents into the vector store
        vectorStore.add(textSplitter.apply(textReader.get()));

        SearchRequest searchRequest = SearchRequest.builder()
            .query(query) // Used for the Vector Search
            .topK(5)            // Number of records to see
            .build();

        // Run similarity search
        List<Document> similaritySearch = vectorStore.similaritySearch(searchRequest);

        // Use an advisor to get an LLM response for the query (demonstration)
        ChatResponse chatResponse = this.builder.build()
            .prompt()
            .user(u -> u.text(query))
            .advisors(QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build())
            .call()
            .chatResponse();

        List<QueryResult> results = new ArrayList<>();

        // Evaluate relevancy using LLM judge
        List<ChunkScore> chunkScores = new ArrayList<>();
        int totalRelevant = 0;
        for (int i = 0; i < similaritySearch.size(); i++) {
            Document doc = similaritySearch.get(i);

            EvaluationRequest evaluationRequest = new EvaluationRequest(
                query,
                List.of(doc),
                chatResponse.getResult().getOutput().getText()
            );
            EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);

            float relevancyScore = evaluationResponse.getScore();
            int chunkIndex = Integer.parseInt(doc.getMetadata().get("chunk_index").toString());
            boolean isRelevant = evaluationResponse.isPass();
            float sufficiencyScore = (float) evaluationResponse.getMetadata().get("sufficiencyScore");

            if(isRelevant)
                totalRelevant++;

            // Collect chunk scoring results including the rank (i+1) so MRR can be computed
            chunkScores.add(
                new ChunkScore(
                    chunkIndex,
                    relevancyScore,
                    isRelevant,
                    i + 1,
                    sufficiencyScore,
                    evaluationResponse.getFeedback()));

            System.out.printf("chunkIndex=%d, relevancyScore=%f, isRelevant=%s\n", chunkIndex, relevancyScore, isRelevant);
        }

        // Single query result (this demo runs one query at a time)
        results.add(new QueryResult(query, chunkScores, similaritySearch.size())); // totalRelevant)); //similaritySearch.size()));

        // Compute aggregated metrics across the (single) result set
        ChunkingMetrics chunkingMetrics = calculateMetrics(results);

        System.out.printf("name=%-15s precision=%-10.4f recall=%-10.4f f1Score=%-10.4f mrr=%-10.4f avgRelevancyScore=%-10.4f%n avgSufficiencyScore=%-10.4f%n",
            strategy.name(),
            chunkingMetrics.precision(),
            chunkingMetrics.recall(),
            chunkingMetrics.f1Score(),
            chunkingMetrics.mrr(),
            chunkingMetrics.avgRelevancyScore(),
            chunkingMetrics.avgSufficiency()
        );

        // Clean up the demo collection to keep environment deterministic between runs
        this.chromaApi.deleteCollection("default_tenant", "default_database", "my_collection");


    }

    /**
     * Compute common ranking and classification metrics used to judge the
     * effectiveness of the chunking strategy.
     *
     * @param results list of per-query {@link QueryResult} objects
     * @return aggregated {@link ChunkingMetrics}
     */
    ChunkingMetrics calculateMetrics(List<QueryResult> results) {
        float totalPrecision = 0.0F;
        float totalRecall = 0.0F;
        float totalF1 = 0.0F;
        float totalMRR = 0.0F;
        float totalRelevancy = 0.0F;
        float totalSufficiency = 0.0F;
        int queryCount = results.size();

        for (QueryResult result : results) {
            // Precision @ K (K=5)
            long relevantRetrieved = result.chunkScores().stream()
                .filter(ChunkScore::isRelevant)
                .count();
            double precision = relevantRetrieved / (double) Math.min(5, result.totalRelevant());

            // Recall @ K
            double recall = relevantRetrieved / (double) result.totalRelevant();

            // F1-Score
            double f1 = (precision + recall) == 0 ? 0 : 2 * (precision * recall) / (precision + recall);

            // Mean Reciprocal Rank
            double mrr = 0.0;
            for (int i = 0; i < result.chunkScores().size(); i++) {
                if (result.chunkScores().get(i).isRelevant()) {
                    mrr = 1.0 / (i + 1);
                    break;
                }
            }

            // Average relevancy score from LLM judge
            double avgRelevancy = result.chunkScores().stream()
                .mapToDouble(ChunkScore::relevancyScore)
                .average()
                .orElse(0.0);

            // Average Sufficiency (LLM Judgment of Chunk Integrity)
            double avgSufficiency = result.chunkScores().stream()
                .mapToDouble(ChunkScore::sufficiencyScore)
                .average()
                .orElse(0.0);

            totalPrecision += precision;
            totalRecall += recall;
            totalF1 += f1;
            totalMRR += mrr;
            totalRelevancy += avgRelevancy;
            totalSufficiency += avgSufficiency;
        }

        return new ChunkingMetrics(
            totalPrecision / queryCount,
            totalRecall / queryCount,
            totalF1 / queryCount,
            totalMRR / queryCount,
            totalRelevancy / queryCount,
            totalSufficiency / queryCount
        );
    }
}
