# KnowledgeGraph Demo

This repository is a small demo that reads documents, extracts subject-predicate-object (SPO) triples via a Chat/LLM integration, stores embeddings in a Neo4j-backed vector store, and writes SPO relationships into Neo4j.

Key components

- `Application.java` - Spring Boot entry point.
- `IngestionPipeline.java` - wires a simple read -> transform -> write pipeline executed at startup.
- `MyDocumentReader.java` - reads `my-file.txt` from the classpath and produces `Document` objects.
- `MyDocumentTransformer.java` - calls a ChatClient to extract SPO triples and attaches them to document metadata under `triples`.
- `MyDocumentWriter.java` - writes documents to the vector store and persists SPO triples into Neo4j (uses `subject`, `predicate`, `object` metadata keys).
- `dto/Triple.java` and `dto/SPOExtraction.java` - DTOs used to map LLM output.

Getting started

Requirements:

- JDK 17+
- Maven
- Neo4j instance accessible to the application (for vector store and graph writes)
- APOC plugin enabled in the Neo4j instance for dynamic relationship creation (or adjust writer to use static relationship types)

Run locally

1. Configure application properties (e.g. `spring.neo4j.uri`, `spring.neo4j.username`, `spring.neo4j.password`) as required by your environment.
2. Ensure APOC is enabled in Neo4j if you rely on `apoc.create.relationship`.
3. Build and run:

```bash
mvn -DskipTests package
java -jar target/*.jar
```

Splitting documents into one line per Document

If you want to ingest the text file line-by-line (one `Document` per line), update `MyDocumentReader` or use the example logic described in code comments to split content on `\R` (Java's line separator regex) and return a `Document` for each non-empty line. This approach is useful when each line contains a discrete record to extract.

LLM prompt and structured extraction

`MyDocumentTransformer` sends a user prompt: `Extract SPO triples from: {text}`. For reliable, structured output that maps to `SPOExtraction`, ensure the Chat client and model are configured to return JSON or a structured format compatible with `BeanOutputConverter<SPOExtraction>`.

Notes

- The wiring in this repo is intentionally minimal to focus on examples. For production use add retries, batching, validation, schema migration, and proper secrets management.
- Logging has been added in critical areas to assist with debugging the pipeline.

License: MIT (adjust as needed)

