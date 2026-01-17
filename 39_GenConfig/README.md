# GenConfig Demo

A small Spring Boot demonstration that shows how to use Spring's AI client bindings
to: (1) generate a JSON extraction template from sample flow files and (2)
extract concrete JSON that follows the generated template.

This repository is intentionally minimal — it wires two ChatClient beans (one
OpenAI-compatible and one Mistral-compatible) and provides a CommandLineRunner
that prints the generated template and extracted JSON to stdout.

Quick facts
- Java: configured for Java 21 in `pom.xml` (adjust if needed)
- Build: Maven
- Main class: `io.forest.genconfig.Application`
- Key configuration class: `io.forest.genconfig.Pipeline`
- Sample inputs: `src/main/resources/complex/*.json` and `src/main/resources/simple/*.json`

What this demo does
1. Loads JSON sample files from `classpath*:complex/*.json` and joins them with `---` separators.
2. Asks a Mistral-backed model to generate a NuExtract-style JSON template from the samples.
3. Asks an OpenAI-style model to extract a concrete JSON instance that adheres to the generated template,
   using a short textual description.
4. Prints the generated template and the extracted JSON to standard output.

Files of interest
- `src/main/java/io/forest/genconfig/Application.java` — Spring Boot entry point
- `src/main/java/io/forest/genconfig/Pipeline.java` — wires AI clients and contains the demo runner
- `src/main/resources/complex/` — example complex flow JSON files used as input samples
- `src/main/resources/simple/` — additional sample JSON files

Configuration

IMPORTANT: The `Pipeline` class currently uses hard-coded API endpoints and keys:
`http://192.168.1.23:1234` and API key `lm-studio` for both the OpenAI- and Mistral-style clients.
Change these values before running against a different server. Two suggested approaches:

- Quick edit: open `Pipeline.openAiChatClient` and `Pipeline.mistralAiChatClient` and replace the
  `baseUrl` and `apiKey` strings with your server and key.

- Better: externalize configuration to `application.yml` and inject with `@Value` or `@ConfigurationProperties`.

Example `application.yml` snippet you can add to `src/main/resources/application.yml`:

```yaml
ai:
  base-url: "http://192.168.1.23:1234"
  api-key: "lm-studio"
  openai:
    model: "nuextract-v1.5"
  mistral:
    model: "mistralai/ministral-3-3b"
```

Then modify `Pipeline` to read these values instead of hard-coding them. This approach is recommended for
production or shared environments.

Build & run

Build the project (skip tests for speed):

```bash
mvn -DskipTests package
```

Run the app using the produced jar:

```bash
java -jar target/*.jar
```

Or run directly with Maven (convenient for development):

```bash
mvn spring-boot:run
```

Generate Javadoc

To generate HTML Javadoc for the project using Maven:

```bash
mvn javadoc:javadoc
```

The generated Javadoc will be available under:

```
target/site/apidocs
```

Troubleshooting & notes

- Java version: The `pom.xml` sets the Java version to 21. If you run into toolchain or runtime errors,
  ensure your environment has Java 21 (or adjust the pom to your installed JDK).
- Model endpoints: The code assumes a local LM server at `192.168.1.23:1234`. If your LM server is remote or
  uses TLS, update the `baseUrl` to the appropriate scheme (`https://...`) and hostname.
- Responses from models: The runner prints raw model outputs to stdout. In production, you should validate
  and/or sanitize these responses before parsing or saving them.
- Warnings in IDE: Because this project is a small demo, some IDEs may flag unused beans or methods as warnings.
  This is expected for demo code that is executed only at runtime through Spring.

Next steps (suggestions)
- Externalize the API endpoint and API key (see `application.yml` example).
- Add unit tests for prompt/response wiring using mocked ChatClient instances.
- Add a small parser/validator that checks model output is valid JSON before attempting to parse.
- Consider adding a simple CLI argument to choose which sample set to use (simple vs complex).

License

Provided as-is for demonstration and learning purposes.

