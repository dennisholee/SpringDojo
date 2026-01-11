package io.forest.kg;

import org.neo4j.driver.Driver;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple ingestion pipeline wiring class.
 *
 * <p>Defines a {@link CommandLineRunner} bean that orchestrates: read -> transform -> write.
 * The pipeline is intentionally small for demo/testing purposes.</p>
 */
@Service
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    /**
     * CommandLineRunner that executes the ingestion pipeline at application startup.
     *
     * <p>This runner reads documents using {@code MyDocumentReader}, applies
     * {@code MyDocumentTransformer} to extract structured information, and then
     * writes the results with {@code MyDocumentWriter}.</p>
     *
     * @param reader      reader that provides documents
     * @param transformer transformer that extracts triples/metadata
     * @param writer      writer that persists documents into stores
     * @return a CommandLineRunner that executes the pipeline
     */
    @Bean
    public CommandLineRunner runner(MyDocumentReader reader,
                                    MyDocumentTransformer transformer,
                                    MyDocumentWriter writer) {
        return args -> {

            log.info("Ingestion pipeline started");
            // Read documents from the configured reader. The reader may return an empty list.
            List<Document> documents = reader.get();
            log.info("Read {} documents", documents == null ? 0 : documents.size());

            // Ensure transformer never receives null; downstream code assumes a list
            List<Document> safeDocuments = documents == null ? List.of() : documents;

            // Transform the documents (e.g., call LLM to extract triples)
            List<Document> transformedDocument = transformer.apply(safeDocuments);
            log.info("Transformed {} documents", transformedDocument == null ? 0 : transformedDocument.size());

            // Write results to vector store and graph
            writer.accept(transformedDocument);
            log.info("Writer accepted {} documents", transformedDocument == null ? 0 : transformedDocument.size());
        };
    }

    /**
     * Create a Neo4jVectorStore bean. The store is configured to initialize its
     * schema (indexes) on startup which is useful for local demos.
     *
     * @param driver         Neo4j driver provided by Spring Boot
     * @param embeddingModel embedding model used by the vector store
     * @return initialized Neo4jVectorStore
     */
    @Bean
    public Neo4jVectorStore vectorStore(Driver driver, EmbeddingModel embeddingModel) {

        log.info("Initializing Neo4jVectorStore (initializeSchema=true)");
        Neo4jVectorStore vectorStore = Neo4jVectorStore.builder(driver, embeddingModel)
            .initializeSchema(true)
            .build();

        // Initialize schema (create index) if needed
        log.info("Neo4jVectorStore initialized");
        return vectorStore;
    }
}
