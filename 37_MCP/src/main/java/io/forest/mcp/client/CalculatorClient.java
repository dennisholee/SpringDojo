package io.forest.mcp.client;

import reactor.core.publisher.Mono;

/**
 * Thin abstraction for components that perform calculator (MCP) tool calls.
 *
 * <p>The implementation is reactive and returns a {@link Mono} that emits a textual representation
 * of the tool result (for example, the sum as a string). Implementations are free to accept a
 * domain payload object (such as {@code CalculatorRequest}) and are responsible for encoding the
 * payload into the MCP call format.</p>
 *
 * Implementations should log errors and return an error signal (Mono.error) when the payload is
 * invalid or a remote call fails.
 */
public interface CalculatorClient {

    /**
     * Perform a calculator tool call using the provided payload.
     *
     * @param payload implementation-defined payload object (e.g. {@link io.forest.mcp.dto.CalculatorRequest})
     * @return a {@link Mono} that emits the tool's textual result or an error signal when the call fails
     */
    Mono<String> callTool(Object payload);
}
