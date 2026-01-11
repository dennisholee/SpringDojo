package io.forest.kg;

import io.forest.kg.dto.SPOExtraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyDocumentTransformer implements DocumentTransformer {

    private final ChatClient chatClient;

//    private final BeanOutputConverter<SPOExtraction> converter;

    private static final Logger log = LoggerFactory.getLogger(MyDocumentTransformer.class);

    public MyDocumentTransformer(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder.defaultAdvisors(
            MessageChatMemoryAdvisor.builder(chatMemory).build()
        ).build();
    //    this.converter = new BeanOutputConverter<>(SPOExtraction.class);
    }

    /**
     * Apply the transformation to every document in the input list. For each
     * document the LLM is prompted to extract SPO triples from the document
     * text. Extracted triples are stored in the document metadata under
     * {@code "triples"}.
     *
     * @param documents list of documents to transform
     * @return the same list of documents with added metadata (triples)
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        log.info("Transforming {} documents", documents == null ? 0 : documents.size());
        return documents.stream()
            .map(doc -> {
                try {
                    log.debug("Sending prompt for document id (metadata subject?): {}", doc.getMetadata().getOrDefault("subject", "<no-subject>"));
                    // Extract triples from document text
                    SPOExtraction extraction = chatClient.prompt()
                        //.advisors(StructuredOutputValidationAdvisor.builder().outputType(Triple.class).build())
                        .user(u -> u
                            .text("Extract SPO triples from the following text and return them as a JSON list under a root 'triples' key: {text}")
                            .param("text", doc.getFormattedContent()))
                        //.system(s -> s.text(this.converter.getFormat()))
                        .call()
                        .entity(new ParameterizedTypeReference<SPOExtraction>(){});
                        //.call()
                        //.entity(new ParameterizedTypeReference<List<Triple>>() {
                        //}); // Maps LLM response to Java object

                    if (extraction == null) { //|| extraction.triples() == null) {
                        log.warn("No triples extracted for document (subject={})", doc.getMetadata().getOrDefault("subject", "<no-subject>"));
                    } else {
                        //log.debug("Extracted {} triples for document (subject={})", extraction.triples().size(), doc.getMetadata().getOrDefault("subject", "<no-subject>"));
                    }

                    // Store extracted triples in document metadata
                    doc.getMetadata().put("triples", extraction); //extraction.triples());
                    return doc;
                } catch (Exception e) {
                    log.error("Error transforming document (subject={})", doc.getMetadata().getOrDefault("subject", "<no-subject>"), e);
                    // Return document unchanged on error
                    return doc;
                }
            }).toList();
    }
}
