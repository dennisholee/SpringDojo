package io.forest.multiagent;

import io.forest.multiagent.mvn.MavenBuildTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Spring configuration that wires named AI agents and a small command-line
 * runner used for quick experimentation.
 *
 * <p>This class defines two AgentService beans (Business Analyst and Solution
 * Architect) and registers a small {@link CommandLineRunner} that demonstrates
 * how to call the configured ChatClient and agents. The heavy prompts used by
 * the agents are stored inline in this example for brevity; in a production
 * system they would typically be externalized to resources or a prompt store.</p>
 */
@Configuration
public class AppConfig {

    /**
     * Defines the Business Analyst agent service bean.
     *
     * @param builder              the ChatClient builder
     * @param toolCallbackProvider the tool callback provider
     * @return the AgentService for the Business Analyst agent
     */
    @Bean
    AgentService businessAnalystAgent(ChatClient.Builder builder,
                                      SyncMcpToolCallbackProvider toolCallbackProvider) {

        return new AgentService(builder,
            toolCallbackProvider,
            new Agent("BusinessAnalyst",
                """
                    <s>[SYSTEM_PROMPT]You are a Senior Technology Business Analyst specializing in Data and System Integration. Your role is to ingest functional requirements and technical design documents and transform them into high-precision Agile User Stories.
                    
                    ### MISSION
                    Translate complex integration logic into modular, "Bot-Ready" Agile stories. The output must be so granular and technically descriptive that a developer bot or AI agent can generate the implementation code (APIs, Data Mappings, Unit Tests) without further human clarification.
                    
                    ### STORY STRUCTURE
                    For every requirement, you must use the following strict format:
                    
                    1. NARRATIVE:
                       - AS A: [Specific Service, Persona, or System Actor]
                       - I WANT: [Specific functional action involving data movement or transformation]
                       - SUCH THAT: [The specific technical or business value achieved]
                    
                    2. ACCEPTANCE CRITERIA (Gherkin Syntax):
                       - Every story must have at least one Happy Path and one Error Handling scenario.
                       - Format: GIVEN [Initial state/precondition], WHEN [Action/Trigger], THEN [Measurable result], AND [Side effects like logging or state changes].
                    
                    3. TECHNICAL SPECIFICATIONS FOR BOTS:
                       - Provide Source/Target field mappings, Data Types, Endpoint URLs, HTTP Methods, and Required Headers.
                       - Define specific Error Codes (e.g., 400 Bad Request, 503 Service Unavailable).
                    
                    ### INTEGRATION GUIDELINES
                    - Focus on Data Contracts: Explicitly state if the data is JSON, XML, or Avro.
                    - Transactional Integrity: Define what happens if a partial failure occurs during a batch or multi-step integration.
                    - Security: Include authentication (OAuth2/mTLS) and encryption requirements in the AC.
                    
                    ### CONSTRAINT
                    - Use "shall" and "must" for mandatory requirements.
                    - If a requirement is ambiguous or missing a data type/mapping, mark it as [MISSING_METADATA: Description].
                    - Do not provide conversational filler. Output only the structured stories.
                    
                    ### OUTPUT RESTRICTIONS
                    - Respond only with the requested technical documentation.
                    - Do not explain your answer or provide a summary of what you did.
                    - Do NOT ask if I need more help or modifications.
                    - Termination: Stop immediately after the last story; any text after the final story is a failure.[/SYSTEM_PROMPT]
                    """,
                List.of()));
    }

    /**
     * Defines the Solution Architect agent service bean.
     *
     * @param builder              the ChatClient builder
     * @param toolCallbackProvider the tool callback provider
     * @return the AgentService for the Solution Architect agent
     */
    @Bean
    AgentService solutionArchitectAgent(ChatClient.Builder builder,
                                        SyncMcpToolCallbackProvider toolCallbackProvider) {

        return new AgentService(builder,
            toolCallbackProvider,
            new Agent("Solution Architect",
                """
                    <s>[SYSTEM_PROMPT]You are a Senior Technology Solution Architect specializing in Enterprise Integration and Data Systems. Your goal is to ingest high-level system requirements and produce a comprehensive "Solution Design Document" (SDD) optimized for downstream consumption by Business Analyst agents.
                    
                    ### MISSION
                    Your output must provide the structural and behavioral blueprint for a technical solution. You must translate business needs into concrete architectural components, data flows, and integration patterns.
                    
                    ### OUTPUT STRUCTURE (The Solution Design Document)
                    Each response must include the following sections:
                    
                    1. ARCHITECTURAL OVERVIEW:
                       - Provide a high-level summary of the chosen integration pattern (e.g., Event-Driven, Hub-and-Spoke, Point-to-Point).
                       - List key components (Microservices, Databases, Message Brokers).
                    
                    2. SYSTEM DIAGRAMS (Mermaid Syntax):
                       - Every design MUST include at least one Mermaid diagram.
                       - For data flow: Use 'graph TD' or 'sequenceDiagram'.
                       - For entity relationships: Use 'erDiagram'.
                       - Ensure all Mermaid code blocks are syntactically correct for immediate rendering.
                    
                    3. DATA CONTRACTS & INTEGRATION SPECIFICATIONS:
                       - Define data schemas (JSON/Avro/XML).
                       - Specify communication protocols (REST, gRPC, MQTT, etc.).
                       - Explicitly list Security protocols (mTLS, OAuth2 scopes, API Keys).
                    
                    4. NON-FUNCTIONAL REQUIREMENTS (NFRs):
                       - Define throughput targets, latency thresholds, and retry/idempotency strategies.
                    
                    ### BA AGENT COMPATIBILITY
                    To ensure the Business Analyst agent can decompose your design into User Stories:
                    - Use clear, unique IDs for every component (e.g., [SRVC-01], [DB-02]).
                    - Explicitly map requirements to specific diagrams.
                    - Detail the "Mapping Logic" required for data transformations.
                    
                    ### CONSTRAINTS
                    - Avoid vague technical jargon; use industry-standard terms.
                    - If a requirement has a conflict (e.g., high availability vs. low cost), flag it as [ARCHITECTURAL_TRADE_OFF] and offer a recommendation.
                    - Ensure all diagrams represent stateful vs stateless components clearly.[/SYSTEM_PROMPT]
                    """,
                List.of()));
    }

    /**
     * CommandLineRunner that demonstrates the usage of the configured ChatClient
     * and agents.
     *
     * @param builder              the ChatClient builder
     * @param toolCallbackProvider the tool callback provider
     * @param businessAnalystAgent the Business Analyst agent service
     * @param solutionArchitectAgent the Solution Architect agent service
     * @return the CommandLineRunner instance
     */
    @Bean
    public CommandLineRunner runner(ChatClient.Builder builder,
                                    SyncMcpToolCallbackProvider toolCallbackProvider,
                                    @Qualifier("businessAnalystAgent") AgentService businessAnalystAgent,
                                    @Qualifier("solutionArchitectAgent") AgentService solutionArchitectAgent) {

        ChatClient chatClient = builder
            .defaultTools(new MavenBuildTool()) // Register the Maven tool
            .defaultAdvisors(ToolCallAdvisor.builder().build()) // Enable autonomous tool calling logic
            .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
            .build();

        return args -> {
            String content = chatClient.prompt()
                .advisors(new SimpleLoggerAdvisor())
                .system(s -> s.text(
                    """
                    <s>[SYSTEM_PROMPT]You are an automated file analyst. You have full permission to use the read_file tool immediately.
                    ### OUTPUT RESTRICTIONS
                    - Respond only with the file content.
                    - Do not provide a summary or explanation.
                    - Do not ask for confirmation or additional help.[/SYSTEM_PROMPT]
                    """
                ))
                .user(u -> u.text(
                    """
                        [INST] Read content from file "/Users/dennislee/Devs/SpringDojo/41_MultiAgent/src/main/resources/requirements/HELLO_WORLD.md". [/INST]
                        """
                ))
                .call()
                .content();

            System.out.printf("--- Requirements found ---\n%s", content);

            Map<String, Object> requirementsMap = Map.of("content", content);

            System.out.println("\n=== Business Analyst Agent ===\n");

            businessAnalystAgent.execute(
                """
                    [INST] ### INPUT 
                    {content}
                    
                    Save the final output to an appropriate filename based on the project goal (e.g., 'payments_sync_spec.md') in the directory: /Users/dennislee/Devs/SpringDojo/41_MultiAgent/src/main/resources/requirements.
                    Output format: Start with the [MISSING_DATA] section, followed by the [TECHNICAL_STORIES]. [/INST]
                    """
                ,
                requirementsMap);

            System.out.println("Solution Architect Agent");

            solutionArchitectAgent.execute(
                """
                    [INST] ### INPUT
                    {content}
                    
                    Generate the design now and save to an appropriate file name under
                    directory /Users/dennislee/Devs/SpringDojo/41_MultiAgent/src/main/resources/requirements. [/INST]
                    """,
                requirementsMap
            );
        };
    }

}
