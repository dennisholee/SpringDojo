package io.forest.genconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Configuration class that wires up AI chat clients and a simple
 * command-line runner demonstrating how those clients are used to
 * generate and extract JSON based on sample flow configuration files.
 *
 * <p>Primary responsibilities:
 * <ul>
 *     <li>Provide two ChatClient beans backed by OpenAI-compatible and Mistral APIs</li>
 *     <li>Provide a CommandLineRunner that loads sample JSON resources and
 *         demonstrates prompting the models to produce templates and extracted JSON</li>
 *     <li>Provide a JDK-based request factory configured with an HttpClient</li>
 * </ul>
 */
@Configuration
public class Pipeline {

    private static final Logger log = LoggerFactory.getLogger(Pipeline.class);

    // Resource resolver used to read sample JSON files from classpath
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     * Creates and configures a ChatClient backed by the OpenAI-style API.
     *
     * <p>Notes:
     * - The baseUrl and apiKey are currently hard-coded for a local LM server
     *   (http://192.168.1.23:1234 / "lm-studio"). Adjust these to point to your
     *   deployment or to use environment-based configuration in production.
     * - The model and generation options are tuned for deterministic extraction
     *   (temperature=0.0) and a large token budget.
     *
     * @param requestFactory low-level HTTP client factory used by the API client
     * @return configured ChatClient instance
     */
    @Bean
    public ChatClient openAiChatClient(JdkClientHttpRequestFactory requestFactory) {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl("http://192.168.1.23:1234")
            .apiKey("lm-studio")
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
            .build();

        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
            .model("nuextract-v1.5")
            .temperature(0.0)
            .maxTokens(8192)
            .build();

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(openAiChatOptions)
            .build();

        return ChatClient.builder(openAiChatModel).build();
    }

    /**
     * Creates and configures a ChatClient backed by the Mistral API.
     *
     * This bean mirrors `openAiChatClient` but targets a different model
     * and model provider. The returned ChatClient is used to generate the
     * extraction template in the demo runner.
     *
     * @param requestFactory HTTP request factory used by the API client
     * @return configured ChatClient instance
     */
    @Bean
    public ChatClient mistralAiChatClient(JdkClientHttpRequestFactory requestFactory) {
        MistralAiApi mistralAiApi = MistralAiApi.builder()
            .baseUrl("http://192.168.1.23:1234")
            .apiKey("lm-studio")
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
            .build();

        MistralAiChatOptions mistralAiChatOptions = MistralAiChatOptions.builder()
            .model("mistralai/ministral-3-3b")
            .temperature(0.0)
            .maxTokens(8192)
            .build();

        MistralAiChatModel mistralAiChatModel = MistralAiChatModel.builder()
            .mistralAiApi(mistralAiApi)
            .defaultOptions(mistralAiChatOptions)
            .build();

        return ChatClient.builder(mistralAiChatModel).build();
    }

    /**
     * CommandLineRunner that demonstrates a simple two-step pipeline:
     * 1) Use the Mistral client to create a JSON template from sample files
     * 2) Use the OpenAI client to extract JSON that follows the template
     *
     * The runner prints both the generated template and the final JSON to
     * standard output so you can inspect the results.
     *
     * @param openAiChatClient the OpenAI-style chat client
     * @param mistralAiChatClient the Mistral chat client
     * @return a CommandLineRunner that executes the demo logic
     */
    @Bean
    public CommandLineRunner runner(@Qualifier("openAiChatClient") ChatClient openAiChatClient,
                                    @Qualifier("mistralAiChatClient") ChatClient mistralAiChatClient) {

        return args -> {

            // Load all JSON files from classpath:complex/*.json and join them with a separator.
            // These samples are provided to the model as demonstration data for template creation.
            Resource[] resources = resolver.getResources("classpath*:complex/*.json");

            String samplesContext = Arrays.stream(resources)
                .map(res -> {
                    try {
                        // Read the content of each resource using UTF-8
                        return res.getContentAsString(StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        // In case of error reading a resource, log and fall back to an empty string
                        log.warn("Failed to read resource {}: {}", res, e.getMessage());
                        return "";
                    }
                })
                .collect(Collectors.joining("\n---\n"));

            // Build a system + user prompt to instruct the Mistral model to produce a
            // NuExtract JSON template based on the sample flows we loaded.
            String template = mistralAiChatClient.prompt()
                .system(s -> s.text(
                    """
                        You are an extremely precise JSON information extractor.
                        Your only task is to extract structured information from the provided text according to the given JSON template/schema.
                        You MUST return ONLY valid JSON - nothing else. No explanations, no markdown, no code fences, no comments, no introductory text.
                        
                        Rules you MUST strictly follow:
                        1. Return ONLY the JSON object - do NOT wrap it in ```json ... ```
                        2. Preserve the exact structure of the provided template including all nesting levels
                        3. Use exactly the same key names as in the template (case-sensitive!)
                        4. If a field is not found → use null (not empty string, not "N/A", not "-", just null)
                        5. For lists/arrays:
                           - return empty array [] when nothing is found
                           - never return null for array fields unless template explicitly shows null as example
                        6. Be very conservative — extract only information that is clearly present
                        7. Do NOT hallucinate, guess, or infer missing values
                        8. Keep original text formatting for names, titles, dates, numbers, codes (do NOT normalize)
                        9. Output must be valid JSON that can be parsed directly
                        """
                ))
                .user(u -> u.text(
                        """
                            Create a NuExtract 1.5 JSON template for generate data based on the samples that are separated by "---".
                            
                            Samples:
                            {samplesContext}
                            
                            JSON Response:
                            """
                    ).param("samplesContext", samplesContext)
                )
                .call()
                .content();

            // Print the generated template for inspection
            System.out.println(template);

            // Build a second prompt that supplies the generated template plus a textual
            // description of the flow to extract concrete JSON that follows the template.
            String jsonConfig = openAiChatClient.prompt()
                .system(s -> s.text(
                    """
                        You are a precise information extraction engine. 
                        Your task is to extract information from the provided text into a JSON string that strictly follows the provided template structure.
                        
                        ### RULES:
                        1. VERBATIM EXTRACTION: Only extract information that is explicitly stated in the text. Do not summarize, infer, or hallucinate.
                        2. TEMPLATE ADHERENCE: Use the keys and nesting defined in the template. All leaf values must be strings.
                        3. OMIT MISSING DATA: If a JSON attribute defined in the template is not found or is unspecified in the text, you MUST skip that attribute entirely in the final JSON output. Do not return null, empty strings, or "N/A".
                        4. ARRAY HANDLING: If the template indicates a list/array but no items are found, omit the key. If items are found, return them as an array of objects/strings as structured in the template.
                        5. PURE JSON: Output only the valid JSON string. Do not include conversational filler, markdown formatting (unless requested), or explanations.
                        6. TEMPLATE BLANK OMISSION: If an attribute in the template has no corresponding value in the text or is blank, SKIP the attribute entirely.
                        7. TEMPLATE EMPTY OMISSION: If a template value is provided as an empty string ("") or an empty array ([]) and the source text contains no relevant data for it, DO NOT include that key in your output.
                        
                        ### THE PRUNING RULE:
                        1. EMPTY VALUE REMOVAL: Remove any attribute where the value is a blank string (""), an empty list ([]), or an empty object ({}).
                        2. RECURSIVE DELETION: If removing an empty value leaves a parent object or list empty, you must also remove that parent.
                        3. VALID CONTENT ONLY: Only include keys that have actual extracted data from the input text.
                        """
                ))
                .user(u -> u.text(
                            """
                                <|input|>
                                ### Template:
                                {template}
                                ### Text:
                                {description}
                                <|output|>
                                """
                        )
                        .param("template", template)
                        .param("description", """
                            1. Create a flow with id "HelloWorld"
                            2. Set "input" to "FlowInputChannel".
                            3. Include the following steps for the flow:
                                Step 1:
                                  * type "enricher"
                                  * request channel "MyChannel1"
                                  * set propertyMappings with the following key value pair:
                                     - "foo": "bar"
                                     - "baz": "quz"
                                Step 2:
                                  * type "to"
                                  * channel "MyChannel2"
                                Step 3:
                                  * type "serviceActivator"
                                  * bean "weatherService"
                                  * method "getTemperature"
                            """)
                )
                .call()
                .content();

            System.out.println("=".repeat(80));
            System.out.println(jsonConfig);
        };
    }

    /**
     * Provides a JDK-based HTTP request factory wired with a standard HttpClient.
     * This is used by the API client builders above so they reuse the same HTTP
     * configuration and connection behavior.
     *
     * @return configured JdkClientHttpRequestFactory
     */
    @Bean
    public JdkClientHttpRequestFactory requestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        return new JdkClientHttpRequestFactory(httpClient);
    }
}
