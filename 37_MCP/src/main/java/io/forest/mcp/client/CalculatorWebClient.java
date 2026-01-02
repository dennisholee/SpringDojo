package io.forest.mcp.client;

import io.forest.mcp.dto.CalculatorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * A simple WebClient-based implementation of {@link CalculatorClient} used for development/testing.
 *
 * <p>This client encodes a {@link CalculatorRequest} into the MCP HTTP payload shape and posts it
 * to the configured MCP server. It expects the server to respond with JSON or text that contains
 * an integer result which will be parsed by {@link CalculatorResponseParser}.</p>
 */
// @Service
public class CalculatorWebClient implements CalculatorClient {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorWebClient.class);

    private final CalculatorResponseParser calculatorResponseParser;

    private final WebClient webClient;

    /**
     * Create the web client.
     *
     * @param baseUrl base URL of the MCP server (fallback to http://localhost:8080 if not provided)
     */
    public CalculatorWebClient(@Value("${mcp.server.base-url:http://localhost:8080}") String baseUrl,
                               CalculatorResponseParser calculatorResponseParser) {
        this.calculatorResponseParser = calculatorResponseParser;

        // Build a simple WebClient to POST requests to the MCP server.
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    /**
     * Call the MCP tool 'add' on the server. Returns the integer result as a String.
     * Accepts JSON, plain text or HTML responses and attempts to parse an integer from the body
     * using {@link CalculatorResponseParser}.
     */
    @Override
    public Mono<String> callTool(Object payload) {

        if (payload instanceof CalculatorRequest request) {

            logger.debug("CalculatorClient.add - preparing request for a={}, b={}", request.a(), request.b());

            // Build a JSON-RPC shaped payload expected by the MCP HTTP endpoint.
            Map<String, Object> requestPayload = Map.of(
                "jsonrpc", request.jsonrpc(),
                "id", request.id(),
                "method", request.method(),

                "params", Map.of(
                    "name", request.name(),
                    "arguments", Map.of(
                        "a", request.a(),
                        "b", request.b()
                    )
                )
            );

            return webClient.post()
                .uri("/mcp", Map.of())
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(s -> logger.debug("CalculatorClient.add - raw response: {}", s))
                // Parse raw response into an integer string using the response parser.
                .map(calculatorResponseParser::extractFromMcpResponse)
                .map(String::valueOf)
                .doOnError(ex -> logger.error("CalculatorClient.add - request failed: a={}, b={}, error={}", request.a(), request.b(), ex.getMessage(), ex));
        }

        return Mono.error(() -> new IllegalArgumentException("Invalid type %s".formatted(payload.getClass().getCanonicalName())));
    }

}
