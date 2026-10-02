# ADR-004: Raw Driver / Plain JDBC over Spring Data and JPA

## Problem statement
How can the isolation level at each engine be made explicit and reviewable, rather than an accident
of framework defaults?

## Background
Spring Data MongoDB and JPA introduce their own transaction and flush behaviour, which can obscure —
or even alter — the isolation level in effect. The PoC's value depends on the reader seeing exactly
which isolation level each engine runs, so the adapters use the raw MongoDB sync driver and plain
JDBC respectively.

## Constraints
- The Mongo transaction must use snapshot read concern (engine default) — not lowered.
- CockroachDB must stay at its `SERIALIZABLE` default — the adapter must not lower it.
- No JPA, no Spring Data, no repository abstraction over the transaction handle.

## Assumptions
- `mongodb-driver-sync` (raw) and `org.postgresql` over JDBC are sufficient and stable.
- Plain `DriverManager` with `setAutoCommit(false)` preserves CockroachDB's default isolation.

## Options

| | Raw driver + plain JDBC | Spring Data MongoDB + JPA | Spring Data + explicit isolation config | ORM abstraction |
| --- | --- | --- | --- | --- |
| Summary | `mongodb-driver-sync` and `DriverManager` | Repositories + `@Transactional` | Framework repositories + manual isolation configuration | Hibernate/JPA across both engines |
| Isolation visible | Yes — in adapter code | No | Partially | No |
| Framework leakage into core | None | Risk | Risk | High |
| Score (out of 5) | 5 | 2 | 3 | 1 |
| Remarks | Chosen. | Rejected: hides the very property under test. | Possible, but unnecessary ceremony. | Rejected: defeats the purpose. |

## Consequences
- The isolation level at each engine is visible in a handful of lines of adapter code.
- CockroachDB's `40001` serialization failure is asserted directly, without Spring exception
  mapping (`CannotSerializeTransactionException`).
- The `core` module stays free of framework dependencies (see
  [ADR-002](ADR-002-engine-agnostic-core.md)).
