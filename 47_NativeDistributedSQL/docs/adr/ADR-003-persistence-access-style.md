# ADR-003: Persistence Access Style

## Problem statement
How should the domain reach the distributed SQL database, given that each row's region placement is the data-residency contract?

## Background
Every CDM table is `REGIONAL BY ROW`, so an insert must name `crdb_region` or the row is homed by default. The PoC keeps SQL explicit in `Jdbc*` adapters that implement outbound ports, and `ArchitectureTests` forbids non-adapter code from depending on `java.sql`, Spring JDBC or the MongoDB driver. All access uses the PostgreSQL wire protocol; there is no vendor-specific driver.

## Constraints
- Region pinning must be visible at the point of every insert.
- Strict serializable isolation must be available and not weakened by the access layer.
- Domain code must be testable without a database.

## Assumptions
- The query surface is small and changes slowly.
- The team is fluent in SQL and can maintain hand-written statements.

## Options

| | Explicit SQL over JDBC | JPA / Hibernate | Spring Data JDBC | Retain MongoDB for the core |
| --- | --- | --- | --- | --- |
| Summary | Hand-written SQL in adapters behind ports | ORM maps entities to tables | Repository interfaces derive statements | Document store remains the system of record |
| Description | `NamedParameterJdbcTemplate` with literal SQL per port method, in adapters that exist only inside a module. | Annotated entities plus a session/entity manager; schema and SQL generated. | Interfaces with derived queries and a mapping context over plain JDBC. | Collection-per-aggregate documents with application-managed consistency. |
| Pros | Placement, isolation and query shape are explicit and reviewable; no mapping surprises. | Less boilerplate for large schemas. | Less boilerplate than JPA; closer to SQL. | Schema flexibility; already in place. |
| Cons | Statements are written by hand. | Hides `crdb_region`; schema drift; heavyweight; eager/lazy traps. | Region column is easy to omit; mapping context still abstracts the SQL. | Cannot transact across separate replica sets, so compensation returns. |
| Score (out of 5) | 5 | 2 | 3 | 1 |
| Remarks | Chosen. Region pinning lives in the adapter and is asserted by scenario R1. | Rejected: the residency rule would become implicit. | Viable fallback if the statement count grows substantially. | Rejected: this is the problem being solved. |
