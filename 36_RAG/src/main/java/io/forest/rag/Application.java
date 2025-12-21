package io.forest.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Spring Boot application.
 * <p>
 * This class bootstraps the Spring context used by the demo. The project is
 * a small "hello world" for connecting Spring AI to a local LLM (LM Studio)
 * and Docling for document conversion and embedding generation.
 */
@SpringBootApplication
public class Application {

    /**
     * Application entry point.
     *
     * @param args standard JVM args forwarded to SpringApplication
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}