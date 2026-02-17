package io.forest.springai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;

import java.util.List;


/**
 * A simple document transformer that uses a chat model to extract keyword
 * metadata for documents. This transformer delegates to the built-in
 * {@link KeywordMetadataEnricher} and returns the enriched documents.
 */
public class MyKeywordEnricher implements DocumentTransformer {

    private final ChatModel chatModel;

    public MyKeywordEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Enrich the provided documents with keyword metadata using the configured
     * chat model. The method is intentionally simple and returns the result of
     * the underlying {@link KeywordMetadataEnricher}.
     *
     * @param documents list of documents to enrich
     * @return enriched list of documents with keyword metadata attached
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        KeywordMetadataEnricher metadataEnricher = KeywordMetadataEnricher.builder(this.chatModel)
            .keywordCount(5)
            .build();

        return metadataEnricher.apply(documents);
    }
}
