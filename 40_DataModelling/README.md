# datamodel-demo

This demo shows how to extract a JSON Schema structure from a JSON payload, enhance that schema according to domain requirements, and produce a merged schema that contains both the original (old) attributes and the newly introduced attributes.

Summary

- Extract: infer a JSON Schema structure from a concrete JSON payload or from a POJO that models the payload.
- Enhance: apply domain-driven changes (for example: normalize an address into a separate model, add foreign-key references, add required/format constraints, or map to an existing standard).
- Merge: produce a final JSON Schema that combines the original attributes and the new attributes introduced by the enhancement rules.

Why this repository

The generator used in this project produces schema from Java types (Classes) or by inspecting example payloads. The typical flow for deriving a schema that reflects actual runtime data is:

1. Deserialize your JSON payload into a POJO that matches the payload structure (or use a payload-based inference routine).
2. Generate a schema for the POJO type using the victools jsonschema-generator (or infer a schema directly from the payload with the provided helper).
3. Apply enhancements according to your requirements (for example, replace an address object with a reference to an ISO-standard address model, or add computed fields).
4. Merge the original schema and the enhancements into a final (combined) schema that includes both old and new attributes.

Result format

The produced schema is a JSON object following JSON Schema conventions (this project uses Draft 2020-12 by default for victools). The "combined" result will typically contain:

- The original `properties` the payload had (old attributes)
- New `properties` introduced by the enhancement rules (new attributes)
- Adjusted `required` and `format` arrays reflecting both old and new attributes
- Where appropriate, references (`$ref`) to external models (for normalization) or inline subschemas

Simple example (conceptual)

Given this payload:

```json
{
  "userId": "u12345",
  "email": "john.doe@example.com",
  "age": 29
}
```

A requirement may request that `address` be normalized and instead of embedding a full address object, the final schema should reference an external `Address` model and provide a foreign key such as `addressId`.

- Old schema (derived from payload):

```json
{
  "$schema" : "http://json-schema.org/draft-07/schema#",
  "title" : "UserProfile",
  "description" : "A schema for a user profile",
  "type" : "object",
  "properties" : {
    "userId" : {
      "type" : "string"
    },
    "email" : {
      "type" : "string",
      "format" : "email"
    },
    "age" : {
      "type" : "integer"
    }
  },
  "required" : [ "userId", "email", "age" ]
}
```

- Enhancement (requirement): normalize `address` into a separate `Address` model and add `addressId` as a foreign key (one-to-one relationship).
- Combined (final) schema (conceptual):

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "UserProfileWithAddress",
  "type": "object",
  "properties": {
    "userId": {"type": "string"},
    "email": {"type": "string", "format": "email"},
    "age": {"type": "integer"},
    "addresses": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/Address"
      }
    },
    "primaryAddressId": {
      "type": "string",
      "description": "ISO 20022-compliant address ID as foreign key to the Address object"
    }
  },
  "required": ["userId", "email", "age"],
  "definitions": {
    "Address": {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "ISO20022_Address",
      "type": "object",
      "properties": {
        "addressId": {"type": "string", "format": "uuid"},
        "street": {"type": "string"},
        "city": {"type": "string"},
        "postalCode": {"type": "string"},
        "country": {"type": "string"},
        "category": {"enum": ["home", "work", "personal"]},
        "isPrimary": {"type": "boolean"}
      },
      "required": ["addressId", "street", "city", "postalCode", "country"]
    }
  }
}
```

- Sample payload based on the new schema

```json
{
  "userId": "USR12345",
  "email": "john.doe@example.com",
  "age": 30,
  "addresses": [
    {
      "$ref": "#/definitions/Address",
      "addressId": "ADDR67890-ISO20022",
      "street": "123 Main St",
      "city": "New York",
      "postalCode": "10001",
      "country": "US",
      "category": "home"
    }
  ],
  "primaryAddressId": "ADDR67890-ISO20022"
}
```

Note: the combined schema keeps the original `address` object (if needed for backward compatibility) and also introduces `addressId`. Depending on your migration strategy you may decide to remove the embedded object or mark it as deprecated.

How to use the project

1. Generate a schema for a POJO type

- Create a POJO representing your payload (for example `Payload.java`).
- Use `SchemaGenerator.generateSchema(Payload.class)` to create a Jackson `ObjectNode` that represents the schema.

2. Derive schema directly from a payload (inference helper)

- Use the included helper `JsonToSchemaGenerator.generateSchema(jsonString, title, description)` to infer a schema directly from an example JSON payload.
- This helper is conservative and infers basic types and nested structures from the sample.

3. Apply domain enhancements

- Implement rule logic (a small transformation function) that takes the derived schema and returns an "enhancement" schema or operations (for example, add a `addressId` property or replace `address` with a `$ref`).
- A simple rule can be: "if property `address` exists, add property `addressId` with type `string` and description referencing the external model." 

4. Merge original + enhancement

- Merge `properties` and update the `required` list as necessary. Keep backward compatibility by retaining old attributes unless you intentionally remove them.

Quick commands (macOS / zsh)

- Build the project (requires Maven installed):

```bash
mvn -DskipTests package
```

- Run the example application:

```bash
mvn -DskipTests exec:java -Dexec.mainClass="io.forest.datamodel.Application"
```

Notes and recommendations

- If you want a robust inference across many samples, collect multiple payload examples, infer schemas for each, then merge or generalize (this demo only provides a single-sample inference helper).
- Decide a migration strategy: keep old attributes for backward compatibility, or mark them as deprecated/removed in the combined schema depending on your consumers.
- The project contains `JsonToSchemaGenerator` (payload inference helper) and `Application` (victools-based type schema generation). Use whichever approach best fits your pipeline.

Files of interest

- `src/main/java/io/forest/datamodel/JsonToSchemaGenerator.java` — infer schema from a JSON payload
- `src/main/java/io/forest/datamodel/Application.java` — example using victools to generate schema from a Java type
- `src/main/java/io/forest/datamodel/Pipeline.java` — example runner that demonstrates generating a schema and piping it into an AI prompt for enrichment

If you want me to include a small example rule implementation that performs a specific enhancement (for example: transform `address` into an `addressId` and add a `$ref` to an external schema), tell me the exact rule and I will add it to the project.
