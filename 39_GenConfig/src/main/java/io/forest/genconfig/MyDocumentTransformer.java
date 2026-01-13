package io.forest.genconfig;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MyDocumentTransformer implements DocumentTransformer {

    private final ChatClient chatClient;
    private final BeanOutputConverter<JsonConfig> outputConverter;

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public MyDocumentTransformer(ChatClient.Builder builder) { //, VectorStore vectorStore) {
        this.chatClient = builder.build();

        // this.vectorStore = vectorStore;
        this.outputConverter = new BeanOutputConverter<>(JsonConfig.class);
    }

    @Override
    public List<Document> apply(List<Document> documents) {

        try {
            Resource[] resources = resolver.getResources("classpath*:complex/*.json");

            String samplesContext = Arrays.stream(resources)
                .map(res -> {
                    try {
                        return res.getContentAsString(StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        return "";
                    }
                })
                .collect(Collectors.joining("\n---\n"));

            return documents.stream()
                .map(doc -> {
                    System.out.printf("Processing document: %s%n", doc.getId());
                    return doc;
                })
                .map(doc -> {

                    String jsonConfig = this.chatClient.prompt()
                        .user(u -> u.text(
                                    """
                                        <|input|>
                                        ### Template:
                                        {template}
                                        ### Text:
                                        {description}
                                        <|output|>
                                        """
                                )
                                .param("template", """
                                    {
                                                  "flowId": "",
                                                  "input": "",
                                                  "steps": [
                                                    {
                                                      "type": "",
                                                      "requestChannel": "",
                                                      "propertyMappings" 
                                                                                       { "key": "value" }
                                                                                     ,
                                                      "channel" : ""
                                                    }
                                                  ]
                                                }
                                    """)
                                .param("description", """
                                    1. Create a flow with id "HelloWorld"
                                    2. Set "input" to "FlowInputChannel".
                                    3. Include the following steps for the flow:
                                        Step 1:
                                          * type "enricher" 
                                          * request channel "MyChannel1"
                                          * set property mappings with the following key value pair:
                                             - "foo": "bar"
                                             - "baz": "quz"
                                        Step 2:
                                          * type "to"
                                          * channel "MyChannel2"
                                    """)
                        )
                        .call()
                        .content();
                    //.entity(JsonConfig.class);

                    doc.getMetadata().put("config", jsonConfig);
                    System.out.println("\n-------\n%s\n------\n\n".formatted(jsonConfig));
                    return doc;

                })
                .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
