package io.forest.kg;

import io.forest.kg.dto.SPOExtraction;
import io.forest.kg.dto.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyDocumentWriter implements DocumentWriter {

    private final Neo4jVectorStore vectorStore;
    private final Neo4jClient neo4jClient;
    private static final Logger log = LoggerFactory.getLogger(MyDocumentWriter.class);

    public MyDocumentWriter(Neo4jVectorStore vectorStore, Neo4jClient neo4jClient) {
        this.vectorStore = vectorStore;
        this.neo4jClient = neo4jClient;
    }

    @Override
    public void accept(List<Document> documents) {
        log.info("Writing {} documents: vector store + graph updates", documents == null ? 0 : documents.size());

        // 2. Write to the Graph Structure (SPO relationships)
        int created = 0;
        if (documents != null) {
            for (Document doc : documents) {
                try {
                    SPOExtraction spoExtraction = (SPOExtraction) doc.getMetadata().get("triples");
                    List<Triple> triples = spoExtraction.triples();

                    for (Triple triple : triples) {
                        String subject = (String) triple.subject();
                        String predicate = (String) triple.predicate();
                        String object = (String) triple.object();

                        log.info("Preparing to write subject={}, predicate={} and object={}", subject, predicate, object);

                        if (subject != null && predicate != null && object != null) {
                            this.neo4jClient.query(
                                    """
                                        MERGE (s:Entity {name: $sub})
                                        MERGE (o:Entity {name: $obj})
                                        WITH s, o
                                        CREATE (o)-[rel:$( $pred )]->(s)
                                        RETURN rel
                                        """
                                )
                                .bind(subject).to("sub")
                                .bind(object).to("obj")
                                .bind(predicate.toUpperCase()).to("pred")
                                .run();
                            created++;
                            log.debug("Created relationship: {} -[{}]-> {}", subject, predicate, object);
                        } else {
                            log.debug("Skipping document with incomplete SPO metadata: subject={}, predicate={}, object={}", subject, predicate, object);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error writing document to graph (subject={})", doc.getMetadata().getOrDefault("subject", "<no-subject>"), e);
                }
            }
        }
        log.info("Graph write complete, relationships created: {}", created);
    }

}
