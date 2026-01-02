package io.forest.mcp.dto;

public record CalculatorRequest(
    String jsonrpc,
    String id,
    String method,
    String name,
    int a,
    int b
) {
}
