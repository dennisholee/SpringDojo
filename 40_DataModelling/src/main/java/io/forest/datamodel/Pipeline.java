package io.forest.datamodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.forest.datamodel.JsonToSchemaGenerator.generateSchema;

/**
 * Demo pipeline bean that generates a schema from a sample payload and sends it
 * to an AI prompt for enrichment. This class is a lightweight example; the
 * ChatClient is expected to be provided by the Spring context.
 */
@SuppressWarnings("unused")
@Configuration
public class Pipeline {

    /**
     * Bean that returns a CommandLineRunner which demonstrates schema generation
     * and enrichment via the configured ChatClient.
     *
     * @param builder ChatClient.Builder provided by Spring
     * @return CommandLineRunner demo task
     * @throws JsonProcessingException if schema generation from sample payload fails
     */
    @Bean
    CommandLineRunner runner(ChatClient.Builder builder) throws JsonProcessingException {

        ChatClient chatClient = builder.build();

        String json = """
            {
              "userId": "u12345",
              "email": "john.doe@example.com",
              "age": 29
            }
            """;

        // Infer a schema from the sample payload (single-sample inference)
        String schema = generateSchema(json, "UserProfile", "A schema for a user profile");

        System.out.println(schema);

        String requirement = """
            1. Generate a seperate address json schema based on the ISO 20022 standard.
            2. Enhance the UserProfile JSON Schema to connect to the new address model as a one-to-one relationship only via the foreign key ID in a normalization format.
            """;

        return args -> {

            String content = chatClient.prompt()
                .user(u -> u.text("""
                    You are a data enrichment engine. Transform the input JSON to satisfy the domain requirement and conform to the target JSON Schema.
                    
                            RULES:
                            - Output ONLY valid JSON. No explanations.
                            - Derive missing fields if possible (e.g., compute age from dateOfBirth).
                            - If address/email category is missing, assume "home" or "personal".
                            - Never invent fake IDs or emails. Use placeholders like "unknown@example.com" only if required.
                    
                            Domain Requirement:
                            {requirement}
                    
                            JSON Schema:
                            {schema}
                    
                            Output (valid JSON only):
                    """)
                    .param("schema", schema)
                    .param("requirement", requirement)

                )
                .call()
                .content();

            // For demo we simply print the enriched content. In production you would
            // deserialize and validate it against the target schema or perform further processing.
            System.out.println(content);

        };
    }
}
