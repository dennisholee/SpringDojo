package io.forest.kg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple document reader that reads text from a classpath resource using
 * {@link org.springframework.ai.reader.TextReader} and exposes it as a list
 * of {@link org.springframework.ai.document.Document} objects.
 *
 * <p>The reader demonstrates adding custom metadata (documentType, sourceId)
 * before reading. For line-by-line ingestion (one Document per line), split
 * the document content on {@code "\\R"} and create new Documents for each
 * line (see README for an example).</p>
 */
@Component
public class MyDocumentReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(MyDocumentReader.class);

    /**
     * Read documents from a classpath file named {@code my-file.txt}.
     *
     * @return list of documents read from the resource (never null)
     */
    @Override
    public List<Document> get() {

        Resource resource = new ClassPathResource("my-file.txt");
        TextReader textReader = new TextReader(resource);

        // Add demo metadata before reading; the metadata is propagated to produced Documents
        textReader.getCustomMetadata().put("documentType", "manual");
        textReader.getCustomMetadata().put("sourceId", 12345);

        log.info("Reading documents from resource: {}", resource.getFilename());
        List<Document> documents = textReader.get();

        // Split each document's content by line breaks (\R matches any line terminator)
        List<Document> lineDocs = documents.stream()
            .flatMap(doc -> {
                String content = doc.getText() == null ? "" : doc.getText();
                Map<String, Object> originalMeta = doc.getMetadata() == null ? Map.of() : doc.getMetadata();
                // copy metadata so each line-doc can be modified independently if needed
                Map<String, Object> metaCopy = new HashMap<>(originalMeta);

                return Arrays.stream(content.split("\\R"))
                    .map(line -> {
                        // Create a new Document per line while preserving metadata
                        return new Document(line, metaCopy);
                    });
            })
            .collect(Collectors.toList());

        int count = lineDocs == null ? 0 : lineDocs.size();
        log.info("Read {} line documents from {}", count, resource.getFilename());

        return lineDocs;
    }
}
