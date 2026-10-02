# ADR-002: Engine-Agnostic Core via Ports-and-Adapters

## Problem statement
How should the invariant be expressed once and exercised identically against two engines, so that
the *engine's* isolation behaviour is the only variable under test?

## Background
The PoC compares MongoDB snapshot isolation against CockroachDB strict serializable. If the business
rule were duplicated or entangled with a data-access framework, any observed difference could be
attributed to the application rather than the engine.

## Constraints
- `core` must have no Spring and no database dependency.
- The business rule must be a pure function.
- Each engine supplies its own transaction handle through an outbound port.
- Dependency direction (`core` ← adapter ← `e2e`) is enforced/reviewed in the build.

## Assumptions
- Two adapters (Mongo, Cockroach) are sufficient for the comparison.
- The read/write surface (`Tx`) is stable and minimal enough to implement twice.

## Options

| | Hexagonal core + port | Logic per adapter | Spring Data repository abstraction | Single engine only |
| --- | --- | --- | --- | --- |
| Summary | One pure `SignatoryGuard`; adapters implement `SignatoryStore` | Copy the rule into each adapter | Share a Spring Data repository interface across engines | Test only one engine |
| Isolation explicitness | High | Low (duplicated, driftable) | Hidden behind the framework | N/A |
| Drift risk | None | High | Medium | None (but no comparison) |
| Exercised identically | Yes | No | Partially | N/A |
| Score (out of 5) | 5 | 2 | 3 | 1 |
| Remarks | Chosen. | Rejected: reintroduces the variable being isolated. | Rejected: the framework hides the isolation level (see [ADR-004](ADR-004-raw-driver-jdbc.md)). | Rejected: defeats the purpose. |

## Architecture view

```text
core:  SignatoryStore (outbound port)  <--implemented by--  MongoSignatoryStore / JdbcSignatoryStore
       Tx (read/write surface)                              (raw driver / plain JDBC)
       SignatoryGuard (pure rule)
e2e:   ConcurrentRevoke -> SignatoryStore -> engine
```

## Consequences
- The invariant lives in exactly one place and is exercised identically by both engines.
- Adding an engine requires one new adapter and zero changes to the rule.
- The `core` module carries no transitive framework or database types.
