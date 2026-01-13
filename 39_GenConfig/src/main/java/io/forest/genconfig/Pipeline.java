package io.forest.genconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
public class Pipeline {

    private static final Logger log = LoggerFactory.getLogger(Pipeline.class);


    @Bean
    public CommandLineRunner runner(MyDocumentReader reader,
                                    MyDocumentTransformer transformer,
                                    ChatClient.Builder builder) {

        ChatClient chatClient = builder.build();

        return args -> {

            log.info("Ingestion pipeline started");
            // Read documents from the configured reader. The reader may return an empty list.
            List<Document> documents = reader.get();
            log.info("Read {} documents", documents == null ? 0 : documents.size());

            // Ensure transformer never receives null; downstream code assumes a list
//            List<Document> safeDocuments = documents == null ? List.of() : documents;

//            // Transform the documents (e.g., call LLM to extract triples)
            List<Document> transformedDocument = transformer.apply(documents);
//            log.info("Transformed {} documents", transformedDocument == null ? 0 : transformedDocument.size());

            // Write results to vector store and graph
            JsonConfig config = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS, documents))
                .user(u -> u.text(
                        """
                            You are updating an existing Spring Integration configuration.
                            
                            NEW REQUIREMENTS:
                            {feedback}
                            
                            Apply the changes and return the updated configuration.
                            """
//                        """
//                        You are updating an existing Spring Integration configuration.
//
//                        PREVIOUS CONFIGURATION:
//                        {previousConfig}
//
//                        NEW REQUIREMENTS:
//                        {feedback}
//
//                        Apply the changes and return the updated configuration.
//                        """
                    )
                    // Spring AI 2026 handles the serialization of the Record automatically
                    // .param("previousConfig", transformedDocument)
                    .param("feedback", "Create a REST flow"))
                .call()
                .entity(JsonConfig.class);

            ObjectMapper objectMapper = new ObjectMapper();
            System.out.println(objectMapper.writeValueAsString(config));

        };
    }

}
