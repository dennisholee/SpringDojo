package io.forest.genconfig;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class MyDocumentReader implements DocumentReader {

    @Override
    public List<Document> get() {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = null;
        try {
            resources = resolver.getResources("classpath*:complex/audit-fanout-flow.json");

            return Arrays.stream(resources)
                .map(TextReader::new)
                .map(TextReader::get)
                .flatMap(List::stream)
                .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
