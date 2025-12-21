package io.forest.rag;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.request.target.InBodyTarget;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.client.DoclingServeClientBuilderFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.net.URI;
import java.util.List;

/**
 * DocumentReader implementation that uses a local Docling server to convert a
 * remote HTTP resource into plain text and wrap it in a {@link Document}.
 *
 * <p>This class isolates the Docling-specific conversion logic so it can be
 * reused by ingestion pipelines or tests. It intentionally performs a simple
 * single-document conversion: more advanced use-cases (batching, caching,
 * retries) can be added later.
 */
public class DoclingDocumentReader implements DocumentReader {

    private final DoclingServeApi api;

    private final String targetUrl;

    /**
     * Create a reader for the given Docling base URL and target resource URL.
     *
     * @param baseUrl  the base URL of the Docling conversion server (e.g. http://localhost:5001)
     * @param targetUrl the remote HTTP resource to convert (e.g. a web page)
     */
    public DoclingDocumentReader(String baseUrl, String targetUrl) {
        this.api = DoclingServeClientBuilderFactory.newBuilder().baseUrl(baseUrl).build();
        this.targetUrl = targetUrl;
    }

    /**
     * Fetches the configured target URL via Docling, requests plain-text output,
     * and returns a single {@link Document} containing the extracted text and
     * a metadata entry for the source URL.
     *
     * @return a list containing one Document with the converted text
     * @throws RuntimeException if the Docling conversion fails or returns no content
     */
    @Override
    public List<Document> get() {
        // Build conversion request: fetch the target URL and ask for TEXT output
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
            .source(
                HttpSource.builder()
                    .url(URI.create(targetUrl))
                    .build())
            .options(
                ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.TEXT)
                    .includeImages(true)
                    .build())
            .target(InBodyTarget.builder().build())
            .build();

        // Call Docling sync conversion endpoint
        ConvertDocumentResponse response = api.convertSource(request);

        // Extract plain-text content from response
        String textContent = response.getDocument().getTextContent();

        // Wrap in a Spring-AI Document and attach the source URL as metadata
        return List.of(Document.builder()
            .text(textContent)
            .metadata("source", targetUrl)
            .build());
    }
}
