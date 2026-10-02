# OpenAPI Knowledge Graph Workbench

This project is now an API-first backend and separate SPA workbench for OpenAPI contract lifecycle operations:

- upload and import OpenAPI contracts into a knowledge graph
- update graph data through idempotent merge re-imports
- preview and execute hard prune operations
- support both `in-memory` and `neo4j` graph providers behind the same service boundary

## Current Architecture

- Backend: Spring Boot 4 (`/api/v1/*` endpoints)
- Graph service façade: delegates to pluggable `KnowledgeGraphStore` implementations
- Providers:
  - `in-memory`
  - `neo4j` (default)
- Frontend: standalone SPA in [frontend](frontend)

## API Surface (v1)

- `POST /api/v1/import` - multipart file upload import
- `POST /api/v1/update` - multipart file upload update/merge
- `GET /api/v1/contracts` - list imported contracts
- `GET /api/v1/prune/{apiId}/preview` - prune impact preview
- `DELETE /api/v1/prune/{apiId}?confirm=true` - hard prune commit
- `GET /api/v1/system/status` - runtime status (provider/version/count)
- `GET /api/v1/analysis/lineage` - structural service -> endpoint -> request/response -> schema mapping
- `GET /api/v1/analysis/lineage.csv` - CSV export for structural mapping
- `GET /api/v1/analysis/collisions` - duplicate operationId and method+path collision report

Detailed request and response payloads are documented in [wiki/api-contract.md](wiki/api-contract.md).

## OpenAPI Analysis Workflow

1) Lint contracts before import:

```bash
npx -y @redocly/cli lint wiki/openapi/*.yaml
```

2) Import contracts (example):

```bash
curl -s -X POST "http://localhost:8080/api/v1/import" \
  -F "files=@wiki/openapi/3-3-4-consent-and-privacy-service.yaml" \
  -F "files=@wiki/openapi/3-4-2-retention-and-deletion-service.yaml"
```

3) Retrieve structural lineage mapping:

```bash
curl -s "http://localhost:8080/api/v1/analysis/lineage"
```

4) Retrieve collision report:

```bash
curl -s "http://localhost:8080/api/v1/analysis/collisions"
```

5) Export lineage as CSV:

```bash
curl -s "http://localhost:8080/api/v1/analysis/lineage.csv" -o lineage.csv
```

## Run Backend

```bash
mvn spring-boot:run
```

## Build And Test

```bash
mvn test
mvn -DskipTests package
```

## Start Dependent Services (Docker Compose)

From the repo root:

```bash
docker compose up -d
```

This starts Neo4j on:

- HTTP browser: `http://localhost:7474`
- Bolt: `bolt://localhost:7687`

Credentials:

- username: `neo4j`
- password: `password`

To run the backend against Neo4j:

```bash
export NEO4J_URI=bolt://localhost:7687
export NEO4J_USERNAME=neo4j
export NEO4J_PASSWORD=password
export NEO4J_DATABASE=neo4j
mvn spring-boot:run -Dspring-boot.run.arguments="--app.graph.provider=neo4j"
```

To use an externally hosted LLM service (OpenAI-compatible endpoint), set:

```bash
export SPRING_AI_OPENAI_BASE_URL=https://your-llm-host.example.com/v1
export SPRING_AI_OPENAI_API_KEY=your-api-key
export SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=your-model-id
```

## Run SPA

From the repo root:

```bash
cd frontend
python3 -m http.server 5173
```

Open `http://localhost:5173/index.html`.

By default, the SPA targets `http://localhost:8080/api/v1`.

## Configuration

Main settings are in [src/main/resources/application.yml](src/main/resources/application.yml):

- `app.graph.provider`: `in-memory` or `neo4j`
- `app.graph.neo4j.*`: Neo4j connection settings
- `app.llm.enabled`: `false` for standalone mode, `true` to enable enrichment
- `app.llm.provider`: `noop` (standalone) or `openai-compatible` (hosted LLM)
- `app.api.cors.allowed-origins`: comma-separated SPA origins
- `spring.servlet.multipart.*`: upload limits

### Operating Modes

Standalone mode (default):

```bash
export APP_LLM_ENABLED=false
export APP_LLM_PROVIDER=noop
```

LLM-enabled mode (externally hosted OpenAI-compatible endpoint):

```bash
export APP_LLM_ENABLED=true
export APP_LLM_PROVIDER=openai-compatible
export SPRING_AI_OPENAI_BASE_URL=https://your-llm-host.example.com/v1
export SPRING_AI_OPENAI_API_KEY=your-api-key
export SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=your-model-id
```

Example Neo4j runtime:

```bash
export NEO4J_URI=bolt://localhost:7687
export NEO4J_USERNAME=neo4j
export NEO4J_PASSWORD=password
export NEO4J_DATABASE=neo4j
```

Then set `app.graph.provider=neo4j` in config or external property overrides.

## Key Backend Files

- [src/main/java/io/forest/datamodel/api/ContractController.java](src/main/java/io/forest/datamodel/api/ContractController.java)
- [src/main/java/io/forest/datamodel/api/PruneController.java](src/main/java/io/forest/datamodel/api/PruneController.java)
- [src/main/java/io/forest/datamodel/api/SystemController.java](src/main/java/io/forest/datamodel/api/SystemController.java)
- [src/main/java/io/forest/datamodel/service/KnowledgeGraphService.java](src/main/java/io/forest/datamodel/service/KnowledgeGraphService.java)
- [src/main/java/io/forest/datamodel/service/InMemoryKnowledgeGraphStore.java](src/main/java/io/forest/datamodel/service/InMemoryKnowledgeGraphStore.java)
- [src/main/java/io/forest/datamodel/service/Neo4jKnowledgeGraphStore.java](src/main/java/io/forest/datamodel/service/Neo4jKnowledgeGraphStore.java)

## Decisions

Architecture and rollout decisions are tracked in [wiki/decisions](wiki/decisions).
