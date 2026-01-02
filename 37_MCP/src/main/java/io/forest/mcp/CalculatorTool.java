package io.forest.mcp;

import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CalculatorTool {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorTool.class);

    @McpTool(name = "add", description = "Add two numbers together")
    public int add(@McpToolParam(description = "First number", required = true) int a,
                   @McpToolParam(description = "Second number", required = true) int b) {
        logger.debug("CalculatorTool.add called with a={}, b={}", a, b);
        int result = a + b;
        logger.info("CalculatorTool.add result: {} (from {} + {})", result, a, b);
        return result;
    }

    @McpResource(uri = "config://{key}", name = "Configuration")
    public String getConfig(String key) {
        logger.debug("CalculatorTool.getConfig called for key={}", key);
        String value = "Hello world";
        logger.info("CalculatorTool.getConfig returning for key={}: {}", key, value);
        return value;
    }
}
