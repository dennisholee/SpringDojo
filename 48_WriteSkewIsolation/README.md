# Write-Skew Isolation PoC

A self-contained, engine-agnostic proof of concept that demonstrates **write skew** under
snapshot isolation and its prevention under strict serializable. It exists to drive an
architecture decision: *is the isolation level — not the topology — the property that matters
when choosing between a MongoDB (sharded) cluster and a distributed SQL engine?*

## 1. Purpose

Prove, deterministically and side-by-side, the same concurrent operation against two engines:

| Engine | Isolation | Expected outcome |
| --- | --- | --- |
| MongoDB (replica set) | snapshot | write skew happens: both transactions commit, invariant silently broken |
| CockroachDB | strict serializable | write skew prevented: one transaction aborts with `SQLSTATE 40001`, invariant holds |

The invariant is a customer-data-master governance rule: **a party (customer) must always keep at
least one authorized signatory.** Signatories are the individuals empowered to act on a party's
accounts; losing the last one leaves the customer unable to operate. It is a *count/absence*
constraint over a party's signatories, so it cannot be protected by a unique index — which is
exactly why write skew is possible under snapshot isolation.

## 2. The phenomenon, in one paragraph

Two transactions each read an overlapping set of rows (both see "party P1 has 2 authorized
signatories"), then each write a *disjoint* row (each revokes only *its own* authorization).
Because they write different rows, snapshot isolation sees no write-write conflict and commits
both. The result (`0` authorized signatories) violates an invariant that no serial order of the two
transactions could have produced. Strict serializable databases detect the read-write dependency
cycle and abort one transaction.

## 3. Scope and non-goals

- **In scope:** the isolation behavior only. Two single-node engines, one shared business rule,
  one deterministic concurrency harness, one pair of end-to-end feature files.
- **Theme:** continues the customer-data-master (CDM) domain — the invariant is a `party`
  governance rule (§1). It imports CDM *concepts* (`party`, `signatory`) only, **not** the CDM
  PoC's `REGIONAL BY ROW` / geo-partitioning machinery.
- **Out of scope:** sharding, geo-partitioning, latency, residency, HA, TLS. Write skew is an
  *isolation* property, not a *sharding* property. A MongoDB **sharded cluster** would still
  exhibit the anomaly because sharding does not change snapshot isolation — this PoC demonstrates
  that point without paying for a sharded topology.
- **Not a production reference:** insecure, single-node, disk-backed-on-volume middleware for
  local PoC use only.

## 4. Repository layout (Maven multi-module)

```
write-skew-poc/
├── pom.xml                         # aggregator (packaging=pom) + dependencyManagement
├── README.md                       # this file
├── docker/
│   ├── docker-compose.mongo.yml        # single-node replica set rs0
│   ├── docker-compose.cockroach.yml    # single-node CockroachDB
│   └── init-cockroach.sql              # signatory table + seed
├── core/                           # engine-agnostic invariant + port (zero DB deps)
│   └── src/main/java/.../signatory/
│       ├── SignatoryGuard.java
│       ├── SignatoryStore.java
│       └── Tx.java
├── adapter-mongo/                  # SignatoryStore over mongodb-driver-sync
│   └── src/main/java/.../signatory/MongoSignatoryStore.java
├── adapter-cockroach/              # SignatoryStore over plain JDBC (postgresql)
│   └── src/main/java/.../signatory/JdbcSignatoryStore.java
└── e2e/                            # Cucumber harness + fixtures + features
    ├── src/test/java/.../signatory/
    │   ├── WriteSkewE2EIT.java     # JUnit Platform suite
    │   ├── SignatorySteps.java     # step definitions
    │   ├── MongoFixture.java
    │   ├── CockroachFixture.java
    │   └── ConcurrentRevoke.java   # the deterministic harness
    └── src/test/resources/features/
        ├── mongo_snapshot_isolation.feature
        └── cockroach_serializable.feature
```

Dependency direction, enforced and reviewed in the build:

```
core  <--  adapter-mongo  <--  e2e (test scope)
core  <--  adapter-cockroach  <--  e2e (test scope)
```

- `core` has **no** Spring and **no** DB dependency; the business rule is a pure function.
- Adapters use the **raw** MongoDB sync driver and **plain JDBC** respectively — no Spring Data,
  no JPA — so the isolation level at each engine is explicit and reviewable.

## 5. Middleware (Docker)

### 5.1 MongoDB — single-node replica set

Transactions (and thus snapshot isolation) require a replica set; a single member is enough.
`docker/docker-compose.mongo.yml`:

```yaml
name: write-skew-poc
services:
  mongo:
    image: mongo:7.0
    hostname: mongo
    command: ["--replSet", "rs0", "--bind_ip_all"]
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

  mongo-init:
    image: mongo:7.0
    depends_on:
      - mongo
    entrypoint: ["/bin/bash", "-c"]
    command:
      - |
        set -e
        for i in $$(seq 1 90); do
          mongosh --host mongo:27017 --quiet --eval 'db.runCommand({ ping: 1 }).ok' >/dev/null 2>&1 && break
          sleep 2
        done
        mongosh --host mongo:27017 --quiet --eval \
          'try { rs.initiate({_id: "rs0", members: [{_id: 0, host: "localhost:27017"}]}) } catch (e) { print("rs already initiated") }'
        for i in $$(seq 1 90); do
          mongosh --host mongo:27017 --quiet --eval 'db.hello().isWritablePrimary' 2>/dev/null | grep -q true && exit 0
          sleep 2
        done
        exit 1

volumes:
  mongo-data:
```

### 5.2 CockroachDB — single node

`docker/docker-compose.cockroach.yml`:

```yaml
name: write-skew-poc
services:
  crdb:
    image: cockroachdb/cockroach:latest-v23.2
    hostname: crdb
    command: start-single-node --insecure --store=/cockroach/cockroach-data
    ports:
      - "26257:26257"
      - "8080:8080"
    volumes:
      - crdb-data:/cockroach/cockroach-data

  crdb-init:
    image: cockroachdb/cockroach:latest-v23.2
    depends_on:
      - crdb
    volumes:
      - ./init-cockroach.sql:/scripts/init-cockroach.sql:ro
    entrypoint: ["/bin/bash", "-c"]
    command:
      - |
        set -e
        for i in $$(seq 1 90); do
          ./cockroach sql --insecure --host=crdb:26257 -e "SELECT 1" >/dev/null 2>&1 && break
          sleep 2
        done
        ./cockroach sql --insecure --host=crdb:26257 -f /scripts/init-cockroach.sql

volumes:
  crdb-data:
```

### 5.3 CockroachDB schema

`docker/init-cockroach.sql`:

```sql
CREATE DATABASE IF NOT EXISTS cdm;
USE cdm;

-- A signatory is a governance sub-entity of a CDM party (customer).
CREATE TABLE IF NOT EXISTS signatory (
    id         STRING PRIMARY KEY,
    party_id   STRING NOT NULL,
    authorized BOOL   NOT NULL
);

-- Reset for a deterministic start: party P1, exactly two authorized signatories.
DELETE FROM signatory;
INSERT INTO signatory (id, party_id, authorized) VALUES
    ('alice', 'P1', true),
    ('bob',   'P1', true);
```

> **Isolation note.** CockroachDB defaults to `SERIALIZABLE`. The JDBC adapter must not lower
> it; Spring's `Isolation.DEFAULT` is fine, but this PoC uses plain `DriverManager` so the level
> stays at the engine default and the choice is visible.

## 6. Core contract (`core`)

The invariant lives **once**, here, and is exercised identically by both adapters.

```java
// SignatoryStore.java — the outbound port. Each engine supplies a transaction handle.
public interface SignatoryStore {
    <T> T inTransaction(java.util.function.Function<Tx, T> action);
}
```

```java
// Tx.java — per-transaction read/write surface.
public interface Tx {
    int  countAuthorized(String partyId);                               // consistent read
    void setAuthorized(String partyId, String signatoryId, boolean v);  // write one row/document
}
```

```java
// SignatoryGuard.java — the business rule (pure, engine-agnostic).
public final class SignatoryGuard {
    public static final int MIN_AUTHORIZED = 1;

    /** Revokes a signatory unless doing so would leave the party with none. */
    public static boolean revoke(Tx tx, String partyId, String signatoryId) {
        if (tx.countAuthorized(partyId) <= MIN_AUTHORIZED) {
            return false;         // refuse: would leave the party with no authorized signatory
        }
        tx.setAuthorized(partyId, signatoryId, false);
        return true;
    }
}
```

## 7. Adapters

### 7.1 `adapter-mongo` — snapshot isolation

```java
public final class MongoSignatoryStore implements SignatoryStore {
    private final MongoClient client;

    @Override
    public <T> T inTransaction(Function<Tx, T> action) {
        try (ClientSession session = client.startSession()) {
            session.startTransaction();          // snapshot read concern, majority write concern
            try {
                T result = action.apply(new MongoTx(session));
                session.commitTransaction();
                return result;
            } catch (RuntimeException e) {
                session.abortTransaction();
                throw e;
            }
        }
    }

    private static final class MongoTx implements Tx {
        private final ClientSession session;
        public int countAuthorized(String partyId) {
            return (int) collection().countDocuments(session,
                    Filters.and(Filters.eq("party_id", partyId), Filters.eq("authorized", true)));
        }
        public void setAuthorized(String partyId, String signatoryId, boolean v) {
            collection().updateOne(session, Filters.eq("_id", signatoryId),
                    Updates.set("authorized", v));
        }
    }
}
```

### 7.2 `adapter-cockroach` — strict serializable

```java
public final class JdbcSignatoryStore implements SignatoryStore {
    private final String url;

    @Override
    public <T> T inTransaction(Function<Tx, T> action) {
        try (Connection c = DriverManager.getConnection(url)) {
            c.setAutoCommit(false);              // stays at SERIALIZABLE (engine default)
            try {
                T result = action.apply(new JdbcTx(c));
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw e instanceof RuntimeException r ? r : new RuntimeException(e);
            }
        }
    }

    private static final class JdbcTx implements Tx {
        private final Connection c;
        public int countAuthorized(String partyId) {
            try (PreparedStatement ps = c.prepareStatement(
                     "SELECT count(*) FROM signatory WHERE party_id = ? AND authorized")) {
                ps.setString(1, partyId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            } catch (SQLException e) { throw new RuntimeException(e); }
        }
        public void setAuthorized(String partyId, String signatoryId, boolean v) {
            try (PreparedStatement ps = c.prepareStatement(
                     "UPDATE signatory SET authorized = ? WHERE id = ?")) {
                ps.setBoolean(1, v);
                ps.setString(2, signatoryId);
                ps.executeUpdate();
            } catch (SQLException e) { throw new RuntimeException(e); }
        }
    }
}
```

> CockroachDB's serialization failure surfaces as a `SQLException` with `SQLState == "40001"`
> (Spring maps it to `CannotSerializeTransactionException`). The e2e harness asserts on that code.

## 8. Deterministic concurrency harness (`e2e`)

Write skew only appears on one interleaving (read-read, then write-write). The harness forces it
with a latch, so the result is reproducible rather than a race:

```java
public record Outcome(
        boolean aliceCommitted, boolean bobCommitted,
        Throwable aliceError, Throwable bobError,
        int finalAuthorized) { }

public final class ConcurrentRevoke {
    public static Outcome run(SignatoryStore store, boolean retryOnSerialization) {
        CountDownLatch bothRead = new CountDownLatch(2);
        String partyId = "P1";

        Result alice = new Result();   // {committed, error, revoked}
        Result bob = new Result();

        Thread a = new Thread(() -> attempt(store, partyId, "alice", bothRead, retryOnSerialization, alice));
        Thread b = new Thread(() -> attempt(store, partyId, "bob", bothRead, retryOnSerialization, bob));
        a.start(); b.start(); a.join(); b.join();

        int finalAuthorized = store.inTransaction(tx -> tx.countAuthorized(partyId));
        return new Outcome(alice.committed, bob.committed, alice.error, bob.error, finalAuthorized);
    }

    private static void attempt(SignatoryStore store, String partyId, String signatoryId,
                                CountDownLatch bothRead, boolean retry, Result out) {
        try {
            out.revoked = store.inTransaction(tx -> {
                tx.countAuthorized(partyId);     // READ happens before either write
                bothRead.countDown();
                bothRead.await();
                return SignatoryGuard.revoke(tx, partyId, signatoryId);
            });
            out.committed = true;
        } catch (Exception e) {
            if (retry && isSerializationFailure(e)) {
                out.revoked = retryOnce(store, partyId, signatoryId);  // re-reads count==1, refuses
            }
            out.error = e;
        }
    }
}
```

The `retry` flag exists only to drive the CockroachDB "retry-on-40001" scenario and to show the
application pattern that snapshot isolation lets you skip.

## 9. End-to-end scenarios (pros *and* cons)

### 9.1 `mongo_snapshot_isolation.feature` (`@engine-mongo`)

```gherkin
Feature: Snapshot isolation permits write skew

  Scenario: concurrent resignations commit without conflict but break the invariant
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign as signatories
    Then both requests committed with no error
    And 0 authorized signatories remain        # WRITE SKEW

  Scenario: a hot-row counter closes the gap (application-level mitigation)
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign against a guarded party
    Then exactly one resigns
    And 1 authorized signatory remains
```

The second scenario demonstrates the *cost* of snapshot isolation: to prevent write skew you must
add a shared "counter" document both transactions update, reintroducing a serialization point in
application code.

### 9.2 `cockroach_serializable.feature` (`@engine-cockroach`)

```gherkin
Feature: Strict serializable prevents write skew

  Scenario: concurrent resignations are serialised and the invariant holds
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign as signatories
    Then exactly one commits
    And the other fails with a serialization error (SQLSTATE 40001)
    And 1 authorized signatory remains          # WRITE SKEW PREVENTED

  Scenario: a retry loop preserves the invariant and still succeeds
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign with retry-on-serialization
    Then exactly one resigns and one is refused on the retry
    And 1 authorized signatory remains
```

## 10. Expected results / verdict

| Engine | Isolation | Concurrent-resignation result | Final `authorized` | Verdict |
| --- | --- | --- | --- | --- |
| MongoDB (replica set) | snapshot | both commit, no error | **0** | Write skew observed — invariant silently broken |
| CockroachDB | strict serializable | one commits, one aborts `40001` | **1** | Write skew prevented — invariant held |

> **Verified 2026-09-26** against the real engines: all four scenarios pass, and the observed end
> states match this table exactly (MongoDB `finalAuthorized = 0`, CockroachDB `1`). Traces, committed
> rows before/after, and the raw artifact list are in
> [`docs/reports/write-skew-evidence-2026-09-26.md`](docs/reports/write-skew-evidence-2026-09-26.md).

**Pros/cons the tests surface:**

- **MongoDB (snapshot).** *Pro:* no spurious aborts; simpler client code; no retry loop. *Con:*
  silently permits write skew, so count/aggregate invariants ("at most one primary", "balance ≥ 0")
  need application-level mitigation (hot-row counters, explicit locks).
- **CockroachDB (strict serializable).** *Pro:* the invariant holds with no application
  coordination. *Con:* the application must handle retryable `40001` serialization errors.

**Architecture conclusion:** sharding does **not** change this result. The deciding factor is the
**isolation level**, not the topology. A MongoDB sharded cluster still runs snapshot-isolated
transactions and would therefore still permit write skew.

## 11. Aggregator `pom.xml` skeleton

Root `pom.xml` (packaging `pom`; version management only, no code):

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
  <modelVersion>4.0.0</modelVersion>

  <groupId>io.forest.isolation</groupId>
  <artifactId>write-skew-poc</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
    <module>core</module>
    <module>adapter-mongo</module>
    <module>adapter-cockroach</module>
    <module>e2e</module>
  </modules>

  <properties>
    <java.version>21</java.version>
    <junit-jupiter.version>5.13.4</junit-jupiter.version>
    <cucumber.version>7.34.8</cucumber.version>
    <mongodb-driver.version>5.2.1</mongodb-driver.version>
    <postgresql.version>42.7.4</postgresql.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.mongodb</groupId>
        <artifactId>mongodb-driver-sync</artifactId>
        <version>${mongodb-driver.version}</version>
      </dependency>
      <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>${postgresql.version}</version>
      </dependency>
      <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-java</artifactId>
        <version>${cucumber.version}</version>
        <scope>test</scope>
      </dependency>
      <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-junit-platform-engine</artifactId>
        <version>${cucumber.version}</version>
        <scope>test</scope>
      </dependency>
      <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-picocontainer</artifactId>
        <version>${cucumber.version}</version>
        <scope>test</scope>
      </dependency>
      <dependency>
        <groupId>org.junit.platform</groupId>
        <artifactId>junit-platform-suite</artifactId>
        <version>1.13.4</version>
        <scope>test</scope>
      </dependency>
      <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit-jupiter.version}</version>
        <scope>test</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

Module dependency wiring:

| Module | Packaging | Dependencies |
| --- | --- | --- |
| `core` | jar | none (plain Java) |
| `adapter-mongo` | jar | `core`, `mongodb-driver-sync` |
| `adapter-cockroach` | jar | `core`, `postgresql` |
| `e2e` | jar | `core`, `adapter-mongo`, `adapter-cockroach`, cucumber (java + junit-platform-engine + picocontainer), junit-platform-suite — all `test` scope except `core` |

The `e2e` module exposes one Maven profile, `e2e` (mirroring the parent CDM PoC), that boots both
Compose files, runs the Cucumber suite, and tears down.

## 12. Build and run

```bash
# Both engines, both feature files, verdict table printed/reported
mvn -pl e2e -am -Pe2e test

# One engine only
mvn -pl e2e -am -Pe2e -Dcucumber.filter.tags="@engine-mongo" test
mvn -pl e2e -am -Pe2e -Dcucumber.filter.tags="@engine-cockroach" test

# Plain build: no Docker, no e2e suite (every scenario class ends in *IT)
mvn test
```

The `e2e` module boots both Compose files via fixtures (mirroring a `...Fixture` pattern), waits for
readiness, runs the scenarios through the shared `SignatoryGuard`, and tears down. No mocks, no
in-memory database.

`-am` is required so `core` and the adapters are resolved from the reactor; without it `-pl e2e`
needs a prior `mvn install`. Override the Compose file directory with `-Dwrite-skew.compose.dir`.

**Evidence artifacts.** Each run writes, per scenario, the committed rows before and after the
concurrency, the per-transaction trace, the response and the verdict — into both the Cucumber report
and a consolidated table:

| Artifact | Contents |
| --- | --- |
| `e2e/target/evidence/verdict.md` | consolidated verdict table for the run |
| `e2e/target/cucumber-reports/cucumber.html` | human-readable report, with the evidence attached to each scenario |
| `e2e/target/cucumber-reports/cucumber.json` | machine-readable equivalent, for tooling |

The executed results, with traces and measured counts, are written up in
[`docs/reports/write-skew-evidence-2026-09-26.md`](docs/reports/write-skew-evidence-2026-09-26.md).


## 13. Key versions

- Java 21, Maven 3.9+
- `mongodb-driver-sync` (raw driver, no Spring Data)
- `org.postgresql:postgresql` (runtime, PostgreSQL wire protocol to CockroachDB)
- JUnit Platform + Cucumber 7
- Docker with the Compose v2 plugin

## 14. Decisions to record once built

Open an ADR alongside the code with these rows:

- Chose **"at least one authorized signatory"** as the invariant: a customer-data-master (`party`)
  governance rule that is an absence/count constraint, unprotectable by a unique index
  (write-skew territory), and a 1:1 structural clone of the canonical doctor-on-call example.
- Chose **single-node** engines because the phenomenon is an isolation property; sharding/geo is
  deliberately excluded from scope.
- Chose **raw driver / JDBC** over Spring Data so the isolation level stays explicit.
- The **latch harness** is mandatory: without forcing the read-read interleaving, the Mongo case
  is a flaky race rather than a demonstration.
- A MongoDB **sharded cluster** would not change the outcome: sharding scales writes but does not
  raise the isolation level. The invariant is also *per-party*, so both signatories key to the same
  shard even in a sharded cluster — the transaction would not cross shards.
