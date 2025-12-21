# Spring AI → LM Studio — Hello World embedding demo

This repository is a tiny "hello world" demo that shows how to connect Spring AI's OpenAI-compatible support to a locally running LM Studio instance to generate embeddings.

Purpose
- Demonstrate a minimal setup to call LM Studio's OpenAI-compatible embedding API via Spring AI.
- Provide a small CommandLineRunner that requests an embedding on startup.

What this project does (high level)
1. Reads LM Studio connection settings from `src/main/resources/application.yml`.
2. A small `EmbeddingTestRunner` runs at startup to request an embedding for a sample string and print the result to stdout.

Files / changes of note
- `src/main/resources/application.yml`
  - Contains LM Studio settings; example values used in this demo:

```yaml
spring:
  main:
    web-application-type: none     # change to 'servlet' to enable web endpoints
  ai:
    openai:
      base-url: http://localhost:1234
      api-key: lm-studio
      embedding:
        options:
          model: text-embedding-nomic-embed-text-v1.5
```

- `src/main/java/io/forest/smartvalidator/EmbeddingTestRunner.java`
  - A `CommandLineRunner` that calls `EmbeddingModel.embedForResponse(...)` for a sample string and prints the embedding vector dimension and metadata.

- `src/main/java/io/forest/smartvalidator/Application.java`
  - Standard Spring Boot application entry point.

How to run this demo (quick steps)

1) Make sure LM Studio is running locally and exposes the OpenAI-compatible API on port 1234. In LM Studio, load an embedding-capable model and note its exact model id (use that id in `application.yml`). LM Studio's API is typically available at `http://localhost:1234/v1`.

2)Install Maven if you don't have it (Homebrew on macOS):

```bash
brew update
brew install maven
```

4) Build the project and run. There are two common ways:

- Run with Maven (recommended during development):

```bash
# package and run the jar
mvn -DskipTests package
java -jar target/smart-validator-1.0-SNAPSHOT.jar

# or run directly with the Spring Boot Maven plugin
mvn -DskipTests spring-boot:run
```

- If you want to see the embedding runner output on startup (non-web mode), leave `web-application-type: none` and run the app. 

5) Verify LM Studio connectivity (optional — lists available models):

```bash
curl -s -X GET "http://localhost:1234/v1/models" -H "Authorization: Bearer lm-studio" | head -n 200
```

What you should see
- If LM Studio is reachable and the configured model is available, the embedding runner will print a short log showing the text, embedding dimension, and metadata.
- If LM Studio is not reachable or the model id is incorrect, the runner will catch the error and print a non-fatal message.

Troubleshooting
- zsh: command not found: mvn — install Maven as shown above.
- `Connection refused` — confirm LM Studio is running on localhost:1234 and that the API path is correct.
- `model not found` — verify the model id in `application.yml` matches the model name exposed by LM Studio's `/models` endpoint.