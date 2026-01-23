package io.forest.datamodel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the datamodel-demo project.
 * <p>
 * Starts the Spring context; other demo beans (for schema generation/enrichment)
 * are discovered via component scanning.
 */
@SpringBootApplication
public class Application {

    /**
     * Application entry point. Starts the Spring Boot application.
     *
     * @param args startup arguments (unused)
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
