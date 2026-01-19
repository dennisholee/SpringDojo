package io.forest.multiagent;

import java.util.List;

/**
 * Represents an AI agent configuration used by the application.
 *
 * <p>The Agent record holds the agent's public name, the system prompt that will
 * be applied as the chat system message, and an optional list of advisor/tool
 * registrations that the agent may use when executing requests.</p>
 *
 * Fields:
 * - agentName: A human-readable identifier for this agent (e.g., "BusinessAnalyst").
 * - systemPrompt: The system-level prompt/text that is applied to every chat
 *   session for this agent. This frequently contains role instructions and
 *   output constraints.
 * - advisors: A list of auxiliary advisor/tool descriptors. The type is kept
 *   generic (List<Object>) to remain decoupled from specific advisor
 *   implementations used by Spring AI clients.
 */
public record Agent(
    String agentName,
    String systemPrompt,
    List<Object> advisors
) {
    // The record is intentionally concise; Javadoc documents the fields for
    // downstream developers and Javadoc generation.
}
