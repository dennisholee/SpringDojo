package io.forest.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Simple ingestion pipeline that demonstrates the classic ETL flow for RAG:
 * extract (read a remote document), transform (chunk text appropriately),
 * and load (persist documents into a vector store which computes embeddings).
 *
 * <p>This service is intentionally small and synchronous for clarity. For
 * production usage consider adding retry/backoff, parallelism, batching and
 * observability.
 */
@Service
public class IngestionPipeline {

    private final VectorStore vectorStore;

    /**
     * Construct the pipeline with the VectorStore used to persist processed documents.
     *
     * @param vectorStore the VectorStore implementation to use for embedding and storage
     */
    public IngestionPipeline(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Run the ingestion pipeline for the provided URL.
     *
     * Steps:
     * 1) EXTRACT: use DoclingDocumentReader to fetch and convert the web page to text
     * 2) TRANSFORM: split/ chunk the text using a TokenTextSplitter to fit model windows
     * 3) LOAD: persist the processed documents into the VectorStore
     *
     * @param url the HTTP URL to ingest
     */
    public void runPipeline(String url) {
        // 1. EXTRACT (Using our new Reader)
        DoclingDocumentReader reader = new DoclingDocumentReader("http://localhost:5001", url);
        List<Document> documents = reader.get();

        // 2. TRANSFORM (Chunking for better search accuracy)
        // TokenTextSplitter ensures context fits within LLM window limits
        var pipeline = new TokenTextSplitter(1000, 400, 10, 5, true);
        List<Document> processedDocs = pipeline.apply(documents);

        // 3. LOAD - persist into the vector store which will compute embeddings
        vectorStore.add(processedDocs);
    }
}
