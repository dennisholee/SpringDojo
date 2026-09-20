package io.forest.integrationhub.integrity;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal example transformer demonstrating a pure, idempotent mapping for JSON-like maps.
 */
public final class JsonTransformer {

    private JsonTransformer() {}

    /**
     * Transforms the input map into a new output map.
     * Example rule: rename key `message` -> `text` and copy other keys unchanged.
     * This method does not mutate the input.
     */
    public static Map<String, Object> transform(Map<String, Object> input) {
        Map<String, Object> out = new HashMap<>();
        if (input == null) return out;
        // copy all keys except `message`, and map `message` -> `text`
        for (Map.Entry<String, Object> e : input.entrySet()) {
            if ("message".equals(e.getKey())) continue;
            out.put(e.getKey(), e.getValue());
        }
        if (input.containsKey("message")) {
            out.put("text", input.get("message"));
        }
        return out;
    }
}
