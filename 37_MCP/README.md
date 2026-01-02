MCP Demo (Vaadin + MCP tool)
=================================

Overview
--------
This repository is a small demo that exposes an MCP-style tool on the server (`CalculatorTool`) and provides a minimal Vaadin frontend that calls an MCP client (`CalculatorClient`) to invoke the remote tool `add`.

The demo contains two client approaches:
- `CalculatorMcpClient` — an MCP client that uses the streamable HTTP transport and reads the MCP server base URL from Spring properties (preferred for MCP/AI setups).
- `CalculatorWebClient` — a simple WebClient-based implementation used for development and testing that posts a JSON envelope to the HTTP endpoint.

What you'll find
----------------
- `src/main/java/io/forest/mcp/CalculatorTool.java` — server-side MCP tool implementing `add(int a, int b)`.
- `src/main/java/io/forest/mcp/client/CalculatorClient.java` — interface for calculator clients.
- `src/main/java/io/forest/mcp/client/CalculatorMcpClient.java` — MCP client using the MCP streamable HTTP transport; reads base URL from Spring properties.
- `src/main/java/io/forest/mcp/client/CalculatorWebClient.java` — WebClient-based client (commented out as a Spring bean by default).
- `src/main/java/io/forest/mcp/ui/MainView.java` — Vaadin UI with two input fields and a button to call `add` and display the result inline.
- `pom.xml` — Maven build configuration.

Prerequisites
-------------
- Java 21 JDK
- Maven 3.8+ (or your preferred mvn wrapper)

Quick start
-----------
Build the project:

```bash
mvn -DskipTests package
```

Run the application (dev):

```bash
mvn spring-boot:run
```

Or run the produced jar:

```bash
java -jar target/mcp-demo-1.0-SNAPSHOT.jar
```

Open the Vaadin UI in your browser:

    http://localhost:8080/

Enter integer values for `a` and `b` and press "Add" — the result will appear inline.

Configuration
-------------
The project includes two configuration styles used by the two clients. Use the property that corresponds to the client implementation you enable.

1) MCP streamable HTTP client (CalculatorMcpClient) — preferred

This client reads the MCP server URL from the Spring AI MCP configuration. Set the following property in `application.yml` or via environment variables / command line:

```yaml
spring:
  ai:
    mcp:
      client:
        streamable-http:
          connections:
            localapi:
              url: http://localhost:8080
```

Override at runtime using environment variables (Spring relaxed binding):

```bash
export SPRING_AI_MCP_CLIENT_STREAMABLE_HTTP_CONNECTIONS_LOCALAPI_URL=http://other-host:8080
# or pass as Spring Boot argument
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.ai.mcp.client.streamable-http.connections.localapi.url=http://other-host:8080"
```

2) WebClient-based client (CalculatorWebClient) — legacy / testing

This client uses `mcp.server.base-url`:

```yaml
mcp:
  server:
    base-url: http://localhost:8080
```

You can also set the same via command-line Spring property `--mcp.server.base-url=...` when running.

Server endpoints
----------------
The application exposes the calculator tool HTTP endpoints. The client and the UI call the tool like this (preferred form):

- POST /mcp/tools/add/call
  - Body: `{ "arguments": { "a": <int>, "b": <int> } }`
  - Response: can be JSON, text/plain, or text/html — the client attempts to extract an integer result from the body.

Example curl (direct call to tool endpoint):

```bash
curl -v -X POST http://localhost:8080/mcp/tools/add/call \
  -H "Content-Type: application/json" \
  -d '{"arguments":{"a":3,"b":4}}'
```

Notes on message envelopes
-------------------------
- Some MCP endpoints accept JSON-RPC-shaped envelopes (`jsonrpc`, `id`, `method`, `params`). Others expect a direct tool call payload with just `arguments`.
- If you see errors like "missing handler for notification type: tools/call" it usually means the server interpreted your request as a JSON-RPC notification (no `id`) and there is no notification handler for that method. To fix that, either:
  - POST directly to `/mcp/tools/{toolName}/call` with a body that contains only the `arguments` map (preferred), or
  - send a full JSON-RPC envelope including `jsonrpc` and `id` if the endpoint expects JSON-RPC.

Troubleshooting
---------------
- Error: "missing handler for notification type: tools/call"
  - Cause: request looked like a JSON-RPC notification (no `id`) and the server had no notification handler for that method.
  - Fix: send either a direct tool call (`{"arguments":{...}}`) to `/mcp/tools/<name>/call` or include `jsonrpc` and `id` if using JSON-RPC.

- Error: UnsupportedMediaTypeException / Content type 'text/html;charset=utf-8' not supported for bodyType=java.lang.Integer
  - Cause: attempting to decode a text/html response as a typed JSON body (Integer). This commonly happens if you call the wrong endpoint (for example, a generic `/mcp` endpoint expecting JSON-RPC instead of `/mcp/tools/<name>/call`).
  - Fix: the provided `CalculatorWebClient` reads the response as a String and attempts to parse an integer. Use that approach or ensure the server responds with a content type the client expects.

Logging
-------
There are helpful log statements in critical parts of the code. To enable debug logging for the demo package add to `application.yml`:

```yaml
logging:
  level:
    io.forest.mcp: DEBUG
```

You should see logs for client requests, raw responses, parsing, and UI actions.

Switching to the Spring AI / official MCP client
------------------------------------------------
If you want to replace the WebClient-based approach with the official MCP client provided by Spring AI:
1. Uncomment or add the appropriate MCP starter dependency in `pom.xml` (e.g. `spring-ai-starter-mcp-client` if present).
2. Implement a small adapter that conforms to the `CalculatorClient` interface and delegates to the Spring AI MCP client APIs.
3. Configure the MCP client via `spring.ai.mcp.*` properties (see Configuration section above).

Tests and next steps
--------------------
- Add unit tests for `CalculatorResponseParser` to cover JSON responses, plain text, and HTML fallbacks. This ensures the parser correctly extracts integers from various responses.
- Consider centralizing MCP configuration with `@ConfigurationProperties` for typed validation and clearer defaults.
- Add a README section with environment-specific overrides or a docker-compose snippet if you want to run a local MCP server alongside the app.