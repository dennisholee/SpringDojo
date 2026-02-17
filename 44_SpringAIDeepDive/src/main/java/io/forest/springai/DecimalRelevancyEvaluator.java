package io.forest.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * An LLM-based evaluator that judges whether a given document chunk is
 * relevant and sufficient to support a provided response.
 *
 * <p>This implementation builds a structured prompt and uses a
 * {@link BeanOutputConverter} to parse the model output into {@link ChunkScore}.
 * The evaluator then converts the score into an {@link EvaluationResponse}
 * which contains a boolean pass/fail and numeric score.</p>
 */
public class DecimalRelevancyEvaluator implements Evaluator {

    private final ChatClient chatClient;
    private final BeanOutputConverter<ChunkScore> converter;

    public DecimalRelevancyEvaluator(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.converter = new BeanOutputConverter<>(ChunkScore.class);
    }

    /**
     * Evaluate the relevancy of the provided document list for the given user
     * query and model response. The evaluation returns a pass when the
     * relevancy and sufficiency thresholds are met.
     *
     * @param request evaluation request containing user query, response and context documents
     * @return an {@link EvaluationResponse} with pass/fail, score and metadata (sufficiencyScore)
     */
    @Override
    public EvaluationResponse evaluate(EvaluationRequest request) {

        String template = """
            CRITICAL TASK: Evaluate if the [Context] contains enough CLEAR and READABLE\s
                information to support the [Response].
            
                1. RELEVANCY: Does the response answer the query using the correct topic?
                2. SUFFICIENCY: Is the context readable and complete? If the context consists of broken fragments,\s
                shredded sentences, or "word salad," you MUST provide a Sufficiency Score below 0.3.
            
                Guidelines
                - Return a valid JSON object.
                - The \"reason\" field must be a SINGLE LINE string.
                - Do NOT use newlines, tabs, or special control characters inside the JSON fields.
            
                Query: {query}
                Response: {response}
                Context: {context}
            
                {format}
            """;

        String contextContent = request.getDataList().stream()
            .map(doc -> doc.getText())
            .collect(Collectors.joining(" | "));

        // Generate the structured response using the configured chat client and parse it
        ChunkScore result = chatClient.prompt()
            .user(u -> u.text(template)
                .param("query", request.getUserText())
                .param("response", request.getResponseContent())
                .param("context", contextContent)
                .param("format", converter.getFormat()))
            .call()
            .entity(converter); // Automatically parses into ChunkScore

        // Simple decision rule: require high relevancy and reasonable sufficiency
        boolean isPass = result.relevancyScore() >= 0.9f && result.sufficiencyScore() >= 0.7f;

        return new EvaluationResponse(
            isPass,
            result.relevancyScore(),
            result.reason(),
            Map.of("sufficiencyScore", result.sufficiencyScore()));
    }
}
