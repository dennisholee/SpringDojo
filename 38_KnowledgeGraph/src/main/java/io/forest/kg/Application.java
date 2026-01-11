package io.forest.kg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

/**
 * Main Spring Boot application entry point for the Knowledge Graph demo.
 *
 * <p>Starts the Spring context and initializes beans used by the ingestion
 * pipeline and graph services. Logging is used to report startup progress.</p>
 */
@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    /**
     * Application entry point.
     *
     * @param args command line arguments passed to SpringApplication
     */
    public static void main(String[] args) {

        // Normalize args to a non-null array to satisfy static analyzers
        String[] safeArgs = Objects.requireNonNullElse(args, new String[0]);
        // Log the fact that the app is starting and how many args were provided
        log.info("Starting Spring application with {} args", safeArgs.length);

        // Boot the Spring context; beans (pipeline, services) are created here
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, safeArgs);

        // After startup - report active profiles for operational visibility
        log.info("Application started. Active profiles: {}", String.join(", ", context.getEnvironment().getActiveProfiles()));

//        JenaGraphService graphService = context.getBean(JenaGraphService.class);
//
//        String knowledgeContext = graphService.getKnowledgeContext("123");
//
//        System.out.println("=".repeat(80));
//        System.out.println(knowledgeContext);
//
//        System.out.println(graphService.getResource("/blog/posts/123"));
    }
}
