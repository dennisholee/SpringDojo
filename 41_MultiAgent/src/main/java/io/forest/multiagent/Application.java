package io.forest.multiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the MultiAgent Spring Boot application.
 *
 * <p>Run this class to start the Spring application context which wires the
 * agents and command-line runner defined in {@link AppConfig}.</p>
 *
 * Build & run (from project root):
 * - ./mvnw spring-boot:run   (if mvnw exists)
 * - mvn spring-boot:run      (otherwise)
 */
@SpringBootApplication
public class Application {

    /**
     * Main launcher used by the JVM to start the Spring Boot application.
     *
     * @param args standard command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
