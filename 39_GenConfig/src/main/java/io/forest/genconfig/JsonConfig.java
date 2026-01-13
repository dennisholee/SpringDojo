package io.forest.genconfig;

import java.util.List;
import java.util.Map;

public record JsonConfig(String flowId,
                         String input,  // e.g., "REST Aggregation", "File-to-DB"
                         List<FlowStep> steps,
                         Map<String, String> configurationMetadata
) {
}