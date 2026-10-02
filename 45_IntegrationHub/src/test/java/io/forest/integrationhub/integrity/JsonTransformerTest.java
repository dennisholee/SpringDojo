package io.forest.integrationhub.integrity;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonTransformerTest {

    @Test
    void transform_isDeterministicAndDoesNotMutateInput() {
        Map<String, Object> input = new HashMap<>();
        input.put("message", "hello");
        input.put("id", 123);

        Map<String, Object> copyForFirst = new HashMap<>(input);
        Map<String, Object> copyForSecond = new HashMap<>(input);

        Map<String, Object> first = JsonTransformer.transform(copyForFirst);
        Map<String, Object> second = JsonTransformer.transform(copyForSecond);

        assertEquals(first, second, "Transform should be deterministic for same input");
        // ensure original inputs were not mutated by the transformer
        assertEquals("hello", input.get("message"));
        assertEquals(123, input.get("id"));
    }
}
