# Write-Skew Isolation PoC — Design Document

- Status: Proposed
- Owner: Platform Architecture
- Related: [../README.md](../README.md), [adr/](./adr/)

## 1. Context and purpose

This PoC exists to drive a single architecture decision: when choosing between a MongoDB
(sharded) cluster and a distributed SQL engine for the customer-data-master (CDM) domain, is the
**isolation level** — not the topology — the property that matters?

It proves the answer deterministically by running the *same* concurrent operation side-by-side
against two engines:

| Engine | Isolation | Expected outcome |
| --- | --- | --- |
| MongoDB (single-node replica set) | snapshot | write skew: both transactions commit, invariant silently broken |
| CockroachDB (single node) | strict serializable | write skew prevented: one transaction aborts `SQLSTATE 40001`, invariant holds |

## 2. The invariant

> **A party (customer) must always keep at least one authorized signatory.**

Signatories are the individuals empowered to act on a party's accounts. Losing the last one leaves
the customer unable to operate. The rule is a **count/absence constraint** over a party's
signatories and therefore **cannot be protected by a unique index** — which is exactly why write
skew is possible under snapshot isolation.

```text
party P1 -- has --> [alice (authorized), bob (authorized)]   # count = 2
revoke alice AND revoke bob concurrently                     # each sees count = 2
invariant: count(authorized) >= 1                            # final count must be >= 1
```

## 3. The phenomenon (write skew under snapshot isolation)

Two transactions each read an overlapping set of rows (both see "P1 has 2 authorized
signatories"), then each write a *disjoint* row (each revokes only its own authorization). Because
they write different rows, snapshot isolation sees no write-write conflict and commits both. The
result (`0` authorized) violates an invariant no serial order could have produced.

Strict-serializable engines detect the read-write dependency cycle (serializable snapshot
isolation) and abort one transaction with `SQLSTATE 40001`.

## 4. Architecture overview

Hexagonal (ports-and-adapters), Maven multi-module:

```text
core  <--  adapter-mongo      <--  e2e (test scope)
core  <--  adapter-cockroach  <--  e2e (test scope)
```

- `core` — engine-agnostic invariant + outbound port; **zero** Spring and **zero** DB deps. The
  business rule is a pure function.
- `adapter-mongo` — `SignatoryStore` over the raw `mongodb-driver-sync` driver (snapshot isolation).
- `adapter-cockroach` — `SignatoryStore` over plain JDBC (`postgresql` wire protocol), left at the
  engine's `SERIALIZABLE` default.
- `e2e` — Cucumber/JUnit harness, fixtures, and the deterministic concurrency driver; no mocks, no
  in-memory database.

Dependency direction is enforced and reviewed in the build (ArchUnit in the parent CDM PoC; the
direction itself is a review gate here).

## 5. Component design

### 5.1 Core contract

```java
public interface SignatoryStore {            // outbound port
    <T> T inTransaction(java.util.function.Function<Tx, T> action);
}

public interface Tx {                        // per-transaction read/write surface
    int  countAuthorized(String partyId);   // consistent read
    void setAuthorized(String partyId, String signatoryId, boolean v);
}

public final class SignatoryGuard {          // pure, engine-agnostic rule
    public static final int MIN_AUTHORIZED = 1;

    public static boolean revoke(Tx tx, String partyId, String signatoryId) {
        if (tx.countAuthorized(partyId) <= MIN_AUTHORIZED) {
            return false;                    // refuse: would leave the party with none
        }
        tx.setAuthorized(partyId, signatoryId, false);
        return true;
    }
}
```

### 5.2 Adapters

- **Mongo** — `inTransaction` opens a `ClientSession`, starts a transaction (snapshot read concern,
  majority write concern), applies the action against a `MongoTx`, then commits/aborts.
- **Cockroach** — `inTransaction` uses `DriverManager`, `setAutoCommit(false)` (stays at
  `SERIALIZABLE`), applies the action against a `JdbcTx`, then commits/rolls back. A serialization
  failure surfaces as `SQLException` with `SQLState == "40001"`.

### 5.3 Deterministic concurrency harness

`ConcurrentRevoke.run(store, retryOnSerialization)` uses a `CountDownLatch(2)` so both transactions
**read** before either **writes**, forcing the only interleaving (read-read, then write-write) in
which write skew appears:

```text
alice: countAuthorized(P1) -> 2      bob: countAuthorized(P1) -> 2
       [latch: both read]                  [latch: both read]
alice: setAuthorized(alice,false)    bob: setAuthorized(bob,false)
       [snapshot: no write-write conflict -> both commit]
```

The `retry` flag drives the CockroachDB "retry-on-40001" scenario and demonstrates the application
pattern snapshot isolation lets you skip.

## 6. Middleware topology

Two single-node containers: a one-member MongoDB replica set `rs0` (transactions require a replica
set) and a single-node CockroachDB (`start-single-node --insecure`), each with an init service that
waits for readiness and seeds party `P1` with exactly two authorized signatories (`alice`, `bob`).

## 7. Expected results / verdict

| Engine | Isolation | Concurrent-resignation result | Final `authorized` | Verdict |
| --- | --- | --- | --- | --- |
| MongoDB | snapshot | both commit, no error | **0** | write skew observed — invariant silently broken |
| CockroachDB | strict serializable | one commits, one aborts `40001` | **1** | write skew prevented — invariant held |

**Pros/cons surfaced by the tests:**

- **MongoDB (snapshot).** *Pro:* no spurious aborts; simpler client code; no retry loop. *Con:*
  silently permits write skew, so count/aggregate invariants ("at most one primary",
  "balance ≥ 0") need application-level mitigation (hot-row counters, explicit locks).
- **CockroachDB (strict serializable).** *Pro:* the invariant holds with no application
  coordination. *Con:* the application must handle retryable `40001` serialization errors.

**Architecture conclusion:** sharding does **not** change this result. The deciding factor is the
**isolation level**, not the topology.

## 8. Non-goals and risks

- **Out of scope:** sharding, geo-partitioning, latency, residency, HA, TLS. Write skew is an
  *isolation* property, not a *sharding* property.
- **Not production-ready:** insecure, single-node, disk-backed middleware for local PoC use only.
- **Risk — flakiness:** without the latch, the Mongo scenario is a race, not a demonstration
  (mitigated by the harness; see [ADR-005](adr/ADR-005-deterministic-latch-harness.md)).
- **Risk — misreading the conclusion:** the verdict says nothing about scale, availability, or
  residency; it isolates *one* axis (isolation level).

## 9. Decision traceability

| Decision | ADR |
| --- | --- |
| Choose "at least one authorized signatory" as the invariant | [ADR-001](adr/ADR-001-invariant-choice.md) |
| Engine-agnostic core via ports-and-adapters | [ADR-002](adr/ADR-002-engine-agnostic-core.md) |
| Single-node engines; isolation not topology | [ADR-003](adr/ADR-003-single-node-engines.md) |
| Raw driver / plain JDBC over Spring Data | [ADR-004](adr/ADR-004-raw-driver-jdbc.md) |
| Latch-based deterministic harness | [ADR-005](adr/ADR-005-deterministic-latch-harness.md) |
| Sharding does not change the outcome | [ADR-006](adr/ADR-006-sharding-does-not-change-outcome.md) |
