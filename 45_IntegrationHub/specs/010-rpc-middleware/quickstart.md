# RPC Middleware Quickstart

This quickstart shows how to generate Java sources from the example proto and run a fast local build.

Generate sources (protoc + plugin):

```bash
mvn -DskipTests generate-sources
```

Compile and run tests with quality enforcements enabled:

```bash
env JAVA_TOOL_OPTIONS='' MAVEN_OPTS='' mvn -Denable.quality.enforcements=true verify
```

Notes:
- Protobuf stubs are generated from `specs/010-rpc-middleware/protos/*.proto` into `target/generated-sources`.
- Do not commit generated sources; they are produced during the Maven build.
