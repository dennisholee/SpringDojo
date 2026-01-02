package io.forest.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CalculatorResponseParser {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorResponseParser.class);


    public int extractFromMcpResponse(String s) {
        // Try to parse JSON first
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode root = objectMapper.readTree(s);

            // If the root is a number node, return it
            if (root.isNumber()) {
                int parsed = root.asInt();
                logger.info("CalculatorClient.add - parsed numeric root result={}", parsed);
                return parsed;
            }

            JsonNode result = root.get("result");
            if (result != null) {

                // Common MCP-like shapes: { "content": [ { "text": "..." } ] }
                JsonNode content = result.get("content");
                if (content != null) {
                    if (content.isTextual()) {
                        int parsed = parseFirstInt(content.asText(), s);
                        logger.info("CalculatorClient.add - parsed from content text: {}", parsed);
                        return parsed;
                    }
                    if (content.isArray() && !content.isEmpty()) {
                        JsonNode first = content.get(0);
                        JsonNode textNode = first.get("text");
                        if (textNode != null && textNode.isTextual()) {
                            int parsed = parseFirstInt(textNode.asText(), s);
                            logger.info("CalculatorClient.add - parsed from content[0].text: {}", parsed);
                            return parsed;
                        }
                    }
                }
            }

            // Fall through to regex extraction from the raw string
        } catch (Exception ex) {
            logger.warn("CalculatorClient.add - JSON parse failed, will attempt regex extraction", ex);
        }

        int parsed = parseFirstInt(s, s);
        logger.info("CalculatorClient.add - parsed by regex: {}", parsed);
        return parsed;
    }

    Integer parseFirstInt(String text, String fullResponseForError) {
        Pattern p = Pattern.compile("-?\\d+");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return Integer.valueOf(m.group());
        }
        logger.error("CalculatorResponseParser.parseFirstInt - failed to find integer in response: {}", fullResponseForError);
        throw new RuntimeException("Failed to parse integer from MCP response: '" + fullResponseForError + "'");
    }
}
