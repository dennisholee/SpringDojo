package io.forest.genconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application entry point for the GenConfig demo.
 * <p>
 * This class boots the Spring application context and triggers any
 * auto-configured beans defined in the application (for example, the
 * beans in `Pipeline`). It exists solely to start the application.
 */
@SpringBootApplication
public class Application {

    /**
     * Application main method - starts the embedded Spring container.
     *
     * @param args command-line arguments forwarded to SpringApplication
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
