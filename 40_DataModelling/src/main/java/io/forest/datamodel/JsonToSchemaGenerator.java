package io.forest.datamodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.*;

/**
 * Utility that infers a JSON Schema from a single example JSON payload.
 *
 * <p>This is a conservative, example-based inference helper: it looks at the
 * provided JSON instance and builds a JSON Schema (draft-07 style) describing
 * the observed structure and simple constraints (types, basic formats).
 *
 * <p>Limitations:
 * <ul>
 *   <li>Inference is based on a single sample. For robust schemas across many
 *       examples, infer from multiple payloads and merge results.</li>
 *   <li>The generator makes simple choices for arrays (uses the first non-null
 *       element to infer the item schema).</li>
 *   <li>Required fields are inferred naively: any non-null property observed is
 *       treated as required in this single-sample approach.</li>
 * </ul>
 */
public class JsonToSchemaGenerator {

    // Shared ObjectMapper used for both parsing and building schema nodes.
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Infer a JSON Schema from a JSON string payload.
     *
     * @param jsonString example JSON payload to infer the schema from
     * @param title optional schema title (can be null)
     * @param description optional schema description (can be null)
     * @return pretty-printed JSON Schema string
     * @throws JsonProcessingException when parsing or serializing JSON fails
     */
    public static String generateSchema(String jsonString, String title, String description) throws JsonProcessingException {
        JsonNode root = mapper.readTree(jsonString);
        ObjectNode schema = mapper.createObjectNode();

        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("title", title != null ? title : "GeneratedSchema");
        schema.put("description", description != null ? description : "Auto-generated JSON Schema");
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();
        Set<String> required = new LinkedHashSet<>();

        // Populate properties and required from the example root object
        inferObjectSchema(root, properties, required);

        schema.set("properties", properties);
        if (!required.isEmpty()) {
            schema.putArray("required")
                .addAll(
                    required.stream()
                        .map(TextNode::new)
                        .toList()
                );
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
    }

    /**
     * Inspect an object node and populate the provided `properties` and `required` containers.
     *
     * <p>This routine only works when `node` is an object; otherwise it is a no-op.
     * The method treats any observed non-null property as required (single-sample heuristic).
     */
    private static void inferObjectSchema(JsonNode node, ObjectNode properties, Set<String> required) {
        if (!node.isObject()) {
            return;
        }

        // Use fieldNames() + get(name) to avoid deprecated APIs and keep compatibility
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode value = node.get(fieldName);

            ObjectNode prop = mapper.createObjectNode();

            // Populate prop with type/constraints inferred from the value
            inferTypeAndConstraints(value, prop);

            properties.set(fieldName, prop);

            // Heuristic: if the field exists and is not null in this example, mark as required
            if (!value.isNull()) {
                required.add(fieldName);
            }
        }
    }

    /**
     * Infer a property's type and some basic constraints (format, items, nested properties).
     *
     * <p>Supported inferences:
     * <ul>
     *   <li>string (+ simple format detection: email, date, uri)</li>
     *   <li>integer / number</li>
     *   <li>boolean</li>
     *   <li>array (infers items from the first non-null element)</li>
     *   <li>object (recursively infers nested properties)</li>
     * </ul>
     *
     * The resulting information is written into the provided `prop` ObjectNode.
     */
    private static void inferTypeAndConstraints(JsonNode node, ObjectNode prop) {
        if (node == null || node.isNull()) {
            prop.put("type", "null");
            return;
        }

        if (node.isTextual()) {
            prop.put("type", "string");
            String text = node.asText();

            // Basic textual format detection
            if (text.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                prop.put("format", "email");
            } else if (text.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                prop.put("format", "date");
            } else if (text.matches("^(https?|ftp)://.*$")) {
                prop.put("format", "uri");
            }
        } else if (node.isIntegralNumber()) {
            prop.put("type", "integer");
        } else if (node.isNumber()) {
            prop.put("type", "number");
        } else if (node.isBoolean()) {
            prop.put("type", "boolean");
        } else if (node.isArray()) {
            prop.put("type", "array");

            ArrayNode itemsExamples = (ArrayNode) node;
            if (!itemsExamples.isEmpty()) {
                // Use first non-null element to infer item schema
                JsonNode firstNonNull = null;
                for (JsonNode item : itemsExamples) {
                    if (item != null && !item.isNull()) {
                        firstNonNull = item;
                        break;
                    }
                }

                if (firstNonNull != null) {
                    ObjectNode itemsSchema = mapper.createObjectNode();
                    inferTypeAndConstraints(firstNonNull, itemsSchema);
                    prop.set("items", itemsSchema);
                } else {
                    // Array contained only nulls -> allow null items
                    prop.set("items", mapper.createObjectNode().put("type", "null"));
                }
            } else {
                // Empty array: fallback to object items (could be improved by config)
                prop.set("items", mapper.createObjectNode().put("type", "object"));
            }
        } else if (node.isObject()) {
            prop.put("type", "object");
            ObjectNode nestedProps = mapper.createObjectNode();
            Set<String> nestedRequired = new LinkedHashSet<>();

            // Recursively infer nested object properties
            inferObjectSchema(node, nestedProps, nestedRequired);
            prop.set("properties", nestedProps);
            if (!nestedRequired.isEmpty()) {
                ArrayNode reqArray = prop.putArray("required");
                nestedRequired.forEach(reqArray::add);
            }
        }
    }

    // ────────────────────────────────────────────────
    //                  Demo / Main
    // ────────────────────────────────────────────────
    /**
     * Demo runner that prints an inferred schema for a sample payload.
     *
     * @param args unused
     * @throws JsonProcessingException if parsing/serialization fails
     */
    public static void main(String[] args) throws JsonProcessingException {

        String json = """
            {
              "userId": "u12345",
              "email": "john.doe@example.com",
              "age": 29
            }
            """;

        String schema = generateSchema(json, "UserProfile", "A schema for a user profile");

        System.out.println(schema);
    }
}
