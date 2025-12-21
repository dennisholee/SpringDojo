# Spring AI → LM Studio — Hello World embedding demo

This repository is a tiny "hello world" demo that shows how to connect Spring AI's OpenAI-compatible support to a locally running LM Studio instance to generate embeddings.

Purpose
- Demonstrate a minimal, reproducible setup to call LM Studio's OpenAI-compatible embedding API using Spring AI.
- Provide a small `CommandLineRunner` that requests an embedding on startup and prints the result to stdout.

Quick overview (what this demo does)
1. Reads LM Studio connection settings from `src/main/resources/application.yml`.
2. On application startup a small `EmbeddingTestRunner` requests an embedding for a sample string via Spring AI and prints embedding metadata (length/dimension) to stdout.
3. (Optional) The demo shows how to persist embeddings to a vector store and run a simple similarity search.

Why this is useful
- LM Studio exposes an OpenAI-compatible API; Spring AI's OpenAI-compatible support can call it with minimal configuration.
- This project is intentionally small to demonstrate the core pieces needed to obtain embeddings and validate connectivity.

Files / changes of note
- `src/main/resources/application.yml`
  - Contains LM Studio settings used by Spring AI (base URL, API key, and embedding model id).

- `src/main/java/io/forest/smartvalidator/EmbeddingTestRunner.java`
  - A `CommandLineRunner` that calls Spring AI's `EmbeddingModel.embedForResponse(...)` for a sample string and prints the embedding vector length and some metadata.

- `src/main/java/io/forest/smartvalidator/Application.java`
  - Standard Spring Boot application entry point.

Configuration example
- Add LM Studio connection settings to `src/main/resources/application.yml`. Adjust the `model` value to the exact id returned by LM Studio's `/v1/models` endpoint.

```yaml
spring:
  main:
    web-application-type: none     # keep non-web to see CommandLineRunner output
  ai:
    openai:
      base-url: http://localhost:1234
      api-key: lm-studio
      embedding:
        options:
          model: text-embedding-nomic-embed-text-v1.5
```

Notes about HTTP/1.1
- Some local deployments of LM Studio may require HTTP/1.1. If you observe protocol errors, create or configure the RestClient/HttpClient bean used by Spring AI to force HTTP/1.1 for the LM Studio `base-url`.

Step-by-step guideline — steps taken in this demo
1) Ensure LM Studio is running locally and an embedding-capable model is loaded.
   - Confirm LM Studio exposes an OpenAI-compatible API (typically `http://localhost:1234/v1`).
   - Note the exact model id shown by LM Studio's `/v1/models` endpoint and use it in `application.yml`.

2) Configure `application.yml` as shown above. Set `web-application-type` to `none` to run the `CommandLineRunner` immediately on startup.

3) (Optional) If necessary, add a RestClient/HttpClient bean to enforce HTTP/1.1 for requests to LM Studio.

4) Build and run the project.

- Build and run (Maven, macOS zsh):

```bash
# package and run the jar
mvn -DskipTests package
java -jar target/smart-validator-1.0-SNAPSHOT.jar

# or run directly with the Spring Boot Maven plugin
mvn -DskipTests spring-boot:run
```

5) Verify LM Studio connectivity (optional) — list available models:

```bash
curl -s -X GET "http://localhost:1234/v1/models" -H "Authorization: Bearer lm-studio" | head -n 200
```

6) What you should see
- On successful connection and valid model id, the `EmbeddingTestRunner` prints the sample text and embedding information (vector length/dimension and any metadata).
- If the LM Studio server is unreachable or the model id is wrong, the runner logs the error and the app exits (or prints a non-fatal message depending on runner implementation).

Optional — persist and validate embeddings
- After receiving an embedding vector you can store it in a vector DB (Chromadb, FAISS, Pinecone, etc.) and run a similarity search to validate results.

Chromadb example — create a collection (HTTP example)

```bash
curl -X 'POST' \
  'http://localhost:8000/api/v1/collections?tenant=default_tenant&database=default_database' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "my_collection",
    "configuration": {},
    "metadata": {"description": "My first collection"},
    "get_or_create": true
  }'
```

Troubleshooting
- zsh: command not found: mvn — install Maven (`brew install maven` on macOS).
- Connection refused — ensure LM Studio is running on the configured host/port and the `/v1` endpoints are enabled.
- model not found — verify the model id in `application.yml` matches a model from LM Studio's `/v1/models`.
- HTTP protocol errors — try forcing HTTP/1.1 for the RestClient used by Spring AI.

Next steps / ideas
- Add a small `EmbeddingTestRunner` unit test to mock the embedding response.
- Persist embeddings into a chosen vector store and add an integration test for similarity search.

License
- Use and modify as needed for learning and development purposes.
