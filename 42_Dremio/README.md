# Dremio Semantic Layer + LLM Chat Client (Spring Boot)

This repository demonstrates a minimal Spring Boot application that queries a Dremio semantic layer (virtual dataset / view) and sends the data to an LLM chat client (Spring AI) for analysis. The sample code lives in `src/main/java/io/forest/dremio/Application.java` and a runtime datasource configuration is in `src/main/resources/application.yml`.

This README explains how to introduce and use a Dremio semantic layer to unify multiple data sources (S3/MinIO, MongoDB, databases, CSVs, etc.), how to connect Spring Boot to Dremio (JDBC / Arrow Flight SQL), and recommended patterns for preparing queries and trimming data before sending to a language model.

Summary
- Dremio provides a semantic layer via virtual datasets (VDS) and spaces that let you expose unified views over heterogeneous data.
- Use Dremio's connectors to register data sources (object stores, databases, file systems, NoSQL) and then create virtual datasets that join and transform data.
- Your Spring Boot app can query Dremio using JDBC (Dremio JDBC) or Arrow Flight SQL (Arrow Flight JDBC driver) and then feed summarized results to an LLM chat client.

Contents
- Motivation and architecture
- How Dremio semantic layer helps LLM workflows
- Setup and configuration
  - Dremio: connectors, VDS, permissions
  - Spring Boot configuration (JDBC / Arrow Flight)
- Example: expose `Dev01.demo01.Book_View` and query it
- Best practices: limiting, summarizing, and streaming
- Security and governance
- Troubleshooting
- Next steps

Motivation and architecture

Use case: an LLM chat client needs to answer user questions about data that resides in several places: object storage (CSV/Parquet), a transactional database, and a document store. Instead of coding multiple connectors in the application, create a semantic layer in Dremio:

- Register connectors (S3/MinIO, MongoDB, MySQL/Postgres, etc.).
- Build virtual datasets that join or transform these sources.
- Expose curated views with meaningful names (for example `Dev01.demo01.Book_View`).

The application queries the view and provides the result to the LLM for analysis. This centralizes data logic, keeps SQL/transformations in Dremio, and simplifies the application code.

How Dremio semantic layer helps LLM workflows
- Data shaping: compute joins, filters, and aggregations inside Dremio so the LLM receives only relevant, compact data.
- Access control: Dremio enforces dataset-level permissions and row/column masking when needed.
- Caching & performance: Dremio's reflections can accelerate repeated queries.

Setup and configuration

1) Dremio: add connectors and create virtual datasets
- In the Dremio UI, add connectors for each datasource:
  - Amazon S3 / MinIO: configure endpoint, access key, secret, and region.
  - MongoDB: add MongoDB connector (specify uri, credentials, authentication).
  - Relational DBs: configure PostgreSQL/MySQL connectors as needed.
  - Local FS: register the local data folder for CSV/Parquet.

- Create a new space (for example `Dev01`) and a folder `demo01`.

- Create or import datasets (table or file-based). For example:
  - `s3://bucket/books/` -> dataset `Books` (parquet/csv)
  - Mongo `authors` collection -> dataset `Authors`

- Create a Virtual Dataset (VDS) `Book_View` that joins or transforms the source tables. Example SQL for a VDS:

  SELECT
    b.book_id,
    b.title,
    a.name AS author_name,
    b.published_year
  FROM
    "s3::bucket".Books b
  JOIN
    "mongo::mydb".Authors a
    ON b.author_id = a._id
  WHERE
    b.published_year >= 2000

- Save the VDS under `Dev01.demo01.Book_View`.

2) Configure Dremio access from Spring Boot

You can talk to Dremio via two common methods: Dremio JDBC (classic) or Arrow Flight SQL (faster, columnar). Using Arrow Flight in the example `application.yml`:

src/main/resources/application.yml

```yaml
spring:
  datasource:
    url: jdbc:arrow-flight-sql://<DREMIO_HOST>:32010/?useEncryption=false
    driver-class-name: org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver
    username: <DREMIO_USERNAME>
    password: <DREMIO_PASSWORD>
  main:
    web-application-type: none
    allow-bean-definition-overriding: true
  ai:
    openai:
      base-url: http://<LM_HOST>:<LM_PORT>
      api-key: <KEY>
      chat:
        options:
          model: <model-name>
          max-tokens: 30000
          temperature: 0

logging:
  level:
    org:
      springframework:
        ai: DEBUG
```

Notes:
- For Arrow Flight, add the Arrow Flight JDBC driver jar to your application's classpath. If using Maven, add the appropriate dependencies or include the driver jar in `lib/`.
- For Dremio's own JDBC driver, use Dremio's suggested artifact/jar and the JDBC URL `jdbc:dremio:direct=<host>:31010;...` depending on version.
- Use TLS where possible. If running Arrow Flight behind TLS enable encryption and configure the certificate.

3) Query the semantic layer from code

This repo's `Application.java` demonstrates a minimal approach: use Spring's `JdbcTemplate` to run SQL against `Dev01.demo01.Book_View` and then forward the serialized rows to the LLM.

Key excerpt (conceptual):

- jdbcTemplate.queryForList("SELECT * FROM Dev01.demo01.Book_View")
- Serialize the returned List<Map<String,Object>> into a compact form
- Call the LLM ChatClient with a prompt that includes the serialized data and the user's question

Example flow in `Application.java`:
- Query Dremio VDS
- Build a prompt with a short instruction and a small serialized sample of the rows
- Call the LLM and print the response

Best practices for LLM consumption

1) Never send entire tables to the LLM. Strategies:
- Pre-aggregate in Dremio (GROUP BY / SUM / COUNT) or filter to only relevant rows.
- Limit columns to only those needed for the question.
- Use sampling (e.g. LIMIT 100) and provide the LLM with summary stats (counts, top-k values).
- Use embeddings and a vector store for retrieval-augmented generation (RAG) when you need content search. Only send matched documents to the model.

2) Control token usage
- Convert rows to a compact format. Example: CSV-like rows, short keys, or enumerated bullets.
- Trim long text fields (store them externally and send links or truncated snippets).

3) Streaming / pagination
- For large result sets, stream rows to a file and compute incremental summaries. Send the summaries to the LLM rather than raw rows.

4) Prompt engineering
- Ask the LLM to reason over a schema first, then ask targeted questions over small batches of rows.
- Provide explicit instructions about the structure and units in data (dates, currencies).

Security and governance

- Use Dremio roles and dataset-level permissions to control who (and which service accounts) can access datasets.
- Avoid embedding raw secrets in `application.yml` in source control. Use environment variables, Vault, or Kubernetes secrets.
- Use TLS for JDBC/Flight connections and for your LLM endpoints.
- Audit queries and data egress from Dremio if you send data to external LLM services.

Troubleshooting

- Driver not found: ensure the Arrow Flight or Dremio JDBC driver is on the classpath. If using Maven, add the dependency or copy the jar into `lib/` and add to the classpath.
- Authentication failures: validate username/password and that the account has access to the requested space/dataset.
- SSL/TLS issues: if using `useEncryption=false` for local testing, do not use in production. For TLS, ensure certificates are trusted by the JVM.
- Performance: if queries are slow, enable Dremio reflections for the VDS or push down aggregations.

Example: minimal `Application.java` behavior
- At startup the app runs a query to `Dev01.demo01.Book_View`, serializes the result and asks the LLM: "List books written by Vince".
- This is intentionally minimal; expand it to accept runtime user queries (API) or to run in response to events.

Next steps and recommendations

- Replace the CommandLineRunner demo with a REST endpoint that accepts a user question and runs a carefully parameterized query against a VDS.
- Implement query parameterization: never concatenate user input into SQL—use prepared statements or validate/sanitize.
- Implement summaries/aggregations in Dremio and only send a small set of results to the LLM.
- Consider a hybrid approach:
  - Use Dremio to prepare and filter the data
  - Extract embeddings for textual fields and store them in a vector DB (e.g., Pinecone, Milvus, or an in-house store)
  - Use a retrieval step to find the most relevant documents/rows and then call the LLM with those results

References
- Dremio docs: https://docs.dremio.com
- Arrow Flight JDBC: https://arrow.apache.org
- Spring AI / ChatClient docs: https://spring.io/projects/spring-ai

If you want, I can:
- Add a REST controller that accepts user queries and runs parameterized Dremio queries.
- Add a streaming/pagination example to avoid loading entire result sets into memory.
- Provide a sample Maven dependency snippet for Arrow Flight or Dremio JDBC.

