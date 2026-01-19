MultiAgent
===========

A small Spring Boot example that wires simple AI "agents" using Spring AI's
ChatClient and demonstrates a tool integration for running Maven commands.

Short purpose / what's in `src`
-------------------------------
- `io.forest.multiagent.Agent` — a record describing an agent configuration
  (name, system prompt, advisors).
- `io.forest.multiagent.AgentService` — a thin wrapper around a configured
  `ChatClient` instance used to prompt the agent and return textual responses.
- `io.forest.multiagent.AppConfig` — Spring `@Configuration` that registers the
  sample agents and a demo `CommandLineRunner` used for quick experimentation.
- `io.forest.multiagent.Application` — Spring Boot application entrypoint.
- `io.forest.multiagent.mvn.MavenBuildTool` — a simple tool that runs Maven
  commands (intended for the Spring AI tool-calling demo).

Getting started (macOS / zsh)
-----------------------------
Open a terminal in the project root (where `pom.xml` is located) and run the
following commands exactly as shown.

1) Build the project (skip tests for a faster build):

```bash
./mvnw -q -DskipTests package
# or (if you don't have the wrapper)
# mvn -q -DskipTests package
```

2) Run the application (development):

```bash
# Run using the Maven wrapper
./mvnw spring-boot:run
# or using the installed maven
# mvn spring-boot:run
```

This starts the Spring application and executes the `CommandLineRunner` in
`AppConfig` (if present). Output will be printed to stdout.

3) Package and run the produced JAR (after step 1):

```bash
# After successful package, run the jar produced under target/
java -jar target/*.jar
```

4) Generate Javadoc (produces HTML under `target/site/apidocs`):

```bash
./mvnw javadoc:javadoc
# then open the generated index on macOS
open target/site/apidocs/index.html
```

Notes & troubleshooting
-----------------------
- If `./mvnw` is not executable, make it executable with: `chmod +x mvnw`.
- The sample `CommandLineRunner` in `AppConfig` is intended for local
  experimentation. It uses the configured `ChatClient` and registered
  agents; adapt prompts and file paths as needed.
- Some methods and beans are used reflectively by Spring or by the AI
  tool framework; static analysis may report "unused" warnings even though
  they are required at runtime.

If you want, I can:
- Run a full build now and paste the terminal output.
- Generate the Javadoc site and open it for you.
- Add a small helper script `scripts/run.sh` to run the common commands.


