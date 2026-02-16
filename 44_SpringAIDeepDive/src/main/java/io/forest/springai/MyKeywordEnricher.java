package io.forest.springai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;

import java.util.List;


public class MyKeywordEnricher implements DocumentTransformer {

    private final ChatModel chatModel;

    public MyKeywordEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        KeywordMetadataEnricher metadataEnricher = KeywordMetadataEnricher.builder(this.chatModel)
            .keywordCount(5)
            .build();

        return metadataEnricher.apply(documents);
    }
}
