package io.forest.multiagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

import java.util.Map;

/**
 * A thin wrapper around a configured ChatClient that represents a named AI agent.
 *
 * <p>This service wires the provided ChatClient.Builder together with memory,
 * tool callback providers, and the agent's configured system prompt. It
 * exposes a simple {@link #execute(String, Map)} method for prompting the
 * agent and returning the agent's textual response.</p>
 */
public class AgentService {

    private final ChatClient chatClient;

    /**
     * Construct a new AgentService.
     *
     * @param builder the ChatClient builder used to create a client for this agent
     * @param toolCallbackProvider provider for tool callbacks used by tool-calling advisors
     * @param agent the Agent configuration (name, system prompt, advisors)
     */
    public AgentService(ChatClient.Builder builder,
                        SyncMcpToolCallbackProvider toolCallbackProvider,
                        Agent agent) {
        this.chatClient = builder
            .defaultAdvisors(ToolCallAdvisor.builder().build()) // Enable autonomous tool calling logic
            .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
            .defaultSystem(s -> s.text(agent.systemPrompt()))
            .build();
    }

    /**
     * Execute the agent with a user message and optional structured parameters.
     *
     * @param message the user-visible message or instruction
     * @param params a map of additional parameters available to the model
     * @return the textual content returned by the chat client
     */
    public String execute(String message, Map<String, Object> params) {
        // Build a prompt using the builder created in the constructor. The system
        // message is already set as the default on the client builder, so we only
        // need to add the user input here.
        return this.chatClient.prompt()
            .user(u -> u.text(message).params(params))
            .call()
            .content();
    }

}
