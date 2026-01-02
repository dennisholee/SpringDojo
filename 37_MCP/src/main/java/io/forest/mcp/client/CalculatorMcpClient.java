package io.forest.mcp.client;

import io.forest.mcp.dto.CalculatorRequest;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * MCP-backed implementation of {@link CalculatorClient}.
 *
 * <p>This service constructs an {@link McpAsyncClient} using a streamable HTTP transport and
 * delegates calculator requests (add) to the configured MCP server. The MCP server base URL is
 * provided via Spring property
 * <code>spring.ai.mcp.client.streamable-http.connections.localapi.url</code> and must be set for the
 * bean to initialize successfully.</p>
 *
 * <p>All operations are reactive and return a {@link Mono} that emits the textual result returned by
 * the MCP tool (typically the computed sum as text). The implementation logs critical lifecycle
 * events and errors to aid debugging.</p>
 */
@Service
public class CalculatorMcpClient implements CalculatorClient {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorMcpClient.class);

    // The async MCP client used to perform tool calls.
    private final McpAsyncClient mcpAsyncClient;

    /**
     * Create a new {@code CalculatorMcpClient}.
     *
     * @param baseUrl the base URL of the MCP server, read from
     *                <code>spring.ai.mcp.client.streamable-http.connections.localapi.url</code>
     *                (must not be null)
     *
     * The constructor builds the HTTP transport, creates the async MCP client, and initializes it.
     * If initialization fails the exception is propagated so the application fails fast.
     */
    @Autowired
    public CalculatorMcpClient(@Value("${spring.ai.mcp.client.streamable-http.connections.localapi.url}") String baseUrl) {

        // Informational log to show which MCP endpoint the client will use.
        logger.info("CalculatorMcpClient initializing using MCP baseUrl={}", baseUrl);

        // Build the transport that the MCP async client will use. The transport encapsulates
        // the HTTP connection details for the streamable HTTP MCP implementation.
        HttpClientStreamableHttpTransport clientTransport = HttpClientStreamableHttpTransport.builder(baseUrl)
            .build();

        // Build the async client with a reasonable request timeout.
        mcpAsyncClient = McpClient.async(clientTransport)
            .requestTimeout(Duration.ofSeconds(10))
            .build();

        try {
            // Initialize the MCP client (performs any handshake / registration as needed).
            mcpAsyncClient.initialize();
            logger.info("CalculatorMcpClient mcpAsyncClient initialized successfully");
        } catch (Exception ex) {
            // Initialization failed — log and rethrow to fail bean creation early.
            logger.error("CalculatorMcpClient failed to initialize mcpAsyncClient", ex);
            throw ex;
        }
    }

    /**
     * Call the MCP tool using the provided payload. The payload must be a {@link CalculatorRequest}
     * instance; other types will cause an {@link IllegalArgumentException} to be returned as an
     * error signal in the reactive stream.
     *
     * @param payload expected to be a {@link CalculatorRequest} containing the tool name and the
     *                two operands (a and b). The method will extract these values and call the
     *                MCP tool with a map payload (keys: "a", "b").
     * @return a {@link Mono} that emits the textual result from the MCP tool (for example, the sum)
     *         or completes empty if the result couldn't be parsed. Errors during the MCP call are
     *         logged and propagated through the returned Mono.
     */
    @Override
    public Mono<String> callTool(Object payload) {

        if (payload instanceof CalculatorRequest request) {

            // Debug log to show the input operands before making the remote call.
            logger.debug("CalculatorClient.add - preparing request for a={}, b={}", request.a(), request.b());

            return this.mcpAsyncClient.callTool(
                    new McpSchema.CallToolRequest(
                        request.name(),
                        // The MCP call expects a key/value map representing the tool's inputs.
                        Map.of(
                            "a", request.a(),
                            "b", request.b()
                        )
                    ))
                .doOnSubscribe(s -> logger.debug("CalculatorMcpClient.callTool - invoking MCP tool: {}", request.name()))
                .flatMap(callToolResult -> {
                    if (callToolResult == null) {
                        // Defensive: protect downstream from unexpected null results.
                        logger.warn("CalculatorMcpClient.callTool - received null callToolResult");
                        return Mono.empty();
                    }

                    // The MCP result contains a content list; we expect textual content for the
                    // calculator tool result. Extract the first content item and handle it.
                    var first = callToolResult.content().getFirst();
                    if (first instanceof McpSchema.TextContent content) {
                        logger.debug("CalculatorMcpClient.callTool - received text content: {}", content.text());
                        return Mono.justOrEmpty(content.text());
                    } else {
                        // Unexpected content type — log for diagnostics and complete without a value.
                        logger.warn("CalculatorMcpClient.callTool - unexpected content type: {}", first);
                        return Mono.empty();
                    }
                })
                .doOnError(ex -> {
                    // Log any errors that occur while making the remote call.
                    logger.error("CalculatorMcpClient.callTool - MCP call failed", ex);
                });
        }

        // If the payload type is not supported, return an error in the reactive stream.
        return Mono.error(() -> new IllegalArgumentException("Invalid type %s".formatted(payload.getClass().getCanonicalName())));
    }
}
