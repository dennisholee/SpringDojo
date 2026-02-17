Spring AI Deep Dive Demo

This repository contains a small demo showing how to wire Spring AI building blocks
(together with a vector store and an LLM) to evaluate document chunking and
retrieval quality.

Key features
- Demonstrates document splitting (TokenTextSplitter), vector indexing/search,
  LLM advisors, and LLM-based evaluation.
- Computes Precision@K, Recall, F1, MRR, and average relevancy/sufficiency.

Requirements
- Java 21
- Maven

Build and run tests

To run the unit/integration-style test that exercises the pipeline:

```bash
mvn test
```

To build the project:

```bash
mvn -DskipTests=false package
```

Key classes
- `io.forest.springai.Application` - Spring Boot entrypoint and example chunking profiles
- `io.forest.springai.Pipeline` - Demo pipeline: read, split, index, search, evaluate, metrics
- `io.forest.springai.DecimalRelevancyEvaluator` - LLM-based evaluator that returns structured scores
- `io.forest.springai.MyKeywordEnricher` - Small wrapper to enrich documents with keywords
- `io.forest.springai.ChunkingStrategy`, `ChunkScore`, `QueryResult`, `ChunkingMetrics` - Value objects used for configuration and metrics

Notes
- This demo expects a local or configured vector store / LLM to be available if you run the pipeline
  outside of the test harness. The test in `PipelineTest` is parametrized and designed to be run in a
  configured environment.

Contributions
- Small improvements: add more unit tests for metric edge cases (zero results, division by zero),
  make thresholds configurable, and extract evaluating logic into a service for easier testing.
