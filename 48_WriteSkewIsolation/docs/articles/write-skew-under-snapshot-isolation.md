# Write Skew: Why a NoSQL Snapshot Lets It Through and a Relational Engine Doesn't

> **In one sentence.** Two concurrent resignations each read "2 authorized" and update different
> records: a snapshot-isolated NoSQL database commits both and silently leaves zero signatories, while
> a serializable relational engine aborts one. This article explains why, shows exactly how to
> reproduce it, and gives two working mitigations.

- Status: Draft for review
- Owner: Platform Architecture
- Related (in the repository, not required reading for this article): the design document, the ADR
  set, and the write-skew evidence report

---

> 📷 **Image prompt — overview infographic (top of article)**
>
> ```text
> Generate a clean flat-vector infographic, wide landscape banner (roughly 16:9),
> titled "Write Skew: NoSQL snapshot vs relational serializable". It is a
> single-panel visual summary of a concurrency-anomaly experiment, laid out
> left-to-right as four zones connected by thin arrows:
>
> 1. FAR LEFT - "The invariant": a party card holding two signatories, "alice"
>    and "bob", each tagged "authorized = true", with a rule badge reading
>    "at least 1 authorized signatory must remain".
>
> 2. NEXT - "The race": two parallel transaction arrows labeled "T1 (alice)" and
>    "T2 (bob)" both reading the same snapshot value "countAuthorized = 2", then
>    diverging to update different rows; annotate "disjoint writes - no
>    write-write conflict".
>
> 3. CENTER - "The fork" (the visual climax): the two arrows split into two
>    outcome lanes. TOP lane labeled "NoSQL - snapshot isolation (MongoDB)":
>    both transactions reach "COMMIT", and a final-state box shows
>    "authorized = 0" with a red warning marker "invariant silently broken".
>    BOTTOM lane labeled "Relational - serializable (CockroachDB)": one
>    transaction ends in an "ABORT" badge reading "SQLSTATE 40001", and the
>    final-state box shows "authorized = 1" with a green check "invariant
>    preserved".
>
> 4. FAR RIGHT - "The fix": two small pills, "hot-row counter - make the
>    conflict visible" and "serialization retry loop - absorb the abort".
>
> Style: monochrome palette with a single blue accent for structure; reserve red
> only for the broken-invariant warning and green only for the preserved-invariant
> check. Readable sans-serif labels, generous whitespace, no code snippets, no
> logos, no photo textures.
> ```

## In this article

1. [The invariant a database cannot enforce](#1-the-invariant-a-database-cannot-enforce)
2. [What is write skew?](#2-what-is-write-skew) — the four conditions that must all be true
3. [How the code base is designed to simulate the problem](#3-how-the-code-base-is-designed-to-simulate-the-problem)
4. [The setup, in code and configuration](#4-the-setup-in-code-and-configuration)
5. [The implementation, in depth](#5-the-implementation-in-depth)
6. [The deterministic harness, in depth](#6-the-deterministic-harness-in-depth)
7. [Results](#7-results) — the measured verdict table and the transaction traces
8. [The mitigations, in depth](#8-the-mitigations-in-depth) — a hot-row counter and a retry loop
9. [Reproduce it](#9-reproduce-it)
10. [Finalization notes](#10-finalization-notes)

Image-generation prompts (📷) appear inline — an overview infographic directly above, and one at the
point where each concept it illustrates is explained.

---

## 1. The invariant a database cannot enforce

The rule under test is a governance rule: **a party must always keep at least one authorized
signatory.** Two signatories, `alice` and `bob`, resign at the same instant.

The rule is expressed once, in engine-agnostic code:

```java
public final class SignatoryGuard {

    public static final int MIN_AUTHORIZED = 1;

    public static boolean revoke(Tx tx, String partyId, String signatoryId) {
        if (tx.countAuthorized(partyId) <= MIN_AUTHORIZED) {
            return false;   // refuse: would leave the party with no authorized signatory
        }
        tx.setAuthorized(partyId, signatoryId, false);
        return true;
    }
}
```

This is a **count/absence constraint** ("at least one remains"). It cannot be expressed as a unique
index, a foreign key, or a `CHECK` constraint, because it is a property of the *set* of rows, not of
any single row. The database has nothing to enforce — correctness depends entirely on the isolation
level the engine provides.

---

## 2. What is write skew?

Write skew is a concurrency anomaly in which two transactions each read a common predicate, then
write to **disjoint** sets of data, and both commit — even though the combined effect of their writes
violates a rule that both transactions individually observed as true.

Here, the two transactions are:

| Transaction | Reads | Writes |
| --- | --- | --- |
| T1 (alice) | `countAuthorized(P1)` → **2** | `alice.authorized = false` |
| T2 (bob) | `countAuthorized(P1)` → **2** | `bob.authorized = false` |

Four conditions combine, and **all four are required**:

1. **The reads overlap.** Both transactions read the snapshot as it was *before* either write, so both
   see `2`.
2. **The writes are disjoint.** T1 writes alice's record; T2 writes bob's. There is no write-write
   conflict on any row or document.
3. **Each decision depends on the other's write.** Had T1 seen T2's write, it would have read `1` and
   refused. This is a *read-write dependency cycle*, and it is precisely what snapshot isolation is
   blind to.
4. **Both commit.** The final state is `0` authorized signatories — a state **no serial ordering of
   the two transactions could ever produce**. Run them one after the other and the second always
   reads `1` and refuses.

The key asymmetry: snapshot isolation only detects *write-write* conflicts (condition 2 is where it
looks), so disjoint writes pass through. A serializable isolation level that tracks *read-write*
dependencies detects the cycle in condition 3 even though the writes are disjoint.

> 📷 **Image prompt — technical concept diagram (write-skew causal chain)**
>
> ```text
> Generate a clean flat-vector technical diagram titled "Write skew: the four
> required conditions". Show a vertical timeline with two parallel swimlanes
> labeled "T1 (alice)" and "T2 (bob)", both running against a shared data-store
> box labeled "party P1". Top: both lanes read "countAuthorized(P1) = 2" from a
> snapshot box labeled "snapshot, before either write". Between the reads and the
> writes draw a barrier icon labeled "CountDownLatch(2)". Middle: T1 writes
> "alice.authorized = false", T2 writes "bob.authorized = false", with an
> annotation "disjoint writes — no write-write conflict". Bottom: both lanes
> reach "COMMIT"; a final-state box shows "authorized = 0" with a red warning
> marker "invariant broken". To the right add a small contrasting inset labeled
> "serializable isolation" where one lane ends in "ABORT — SQLSTATE 40001" and
> the final state is "authorized = 1". Number the four conditions as callouts
> (1) overlapping reads, (2) disjoint writes, (3) each decision depends on the
> other's write, (4) both commit. Monochrome palette with one blue accent,
> readable sans-serif labels, no code snippets, no logos.
> ```

---

## 3. How the code base is designed to simulate the problem

The demonstration isolates a single variable — the **isolation level** — and holds everything else
constant. The same rule, the same port, the same harness, and the same test scenarios run against two
engines that differ only in what their default isolation guarantees.

```text
core/               SignatoryGuard (the rule) · SignatoryStore (the port) · Tx (the surface)
adapter-mongo/      MongoSignatoryStore   — snapshot isolation (engine default, never overridden)
adapter-cockroach/  JdbcSignatoryStore    — strict SERIALIZABLE (engine default, never lowered)
e2e/                Cucumber scenarios + the latch harness + the evidence report
docker/             docker-compose files + schema init
```

Three design decisions make the result trustworthy, and each is worth unpacking.

### 3.1 Decision 1 — the invariant lives in an engine-agnostic `core`

The business rule is expressed against two interfaces, not against a driver:

```java
public interface SignatoryStore {
    <T> T inTransaction(Function<Tx, T> action);
}

public interface Tx {
    int  countAuthorized(String partyId);                 // consistent read
    void setAuthorized(String partyId, String signatoryId, boolean authorized); // one row
    void touch(String partyId);                           // the hot-row counter (mitigation)
}
```

`SignatoryGuard` never sees MongoDB or CockroachDB. It only sees `Tx`. This is what makes the
comparison honest: the *same* `SignatoryGuard.revoke` runs on both engines, so any difference in
outcome is attributable to the adapter — and therefore to the isolation level — not to the rule.

### 3.2 Decision 2 — raw drivers, so the isolation level stays visible

The adapters use the MongoDB sync driver and plain JDBC directly. There is no ORM or framework that
could quietly lower the isolation level. In the JDBC adapter the choice is explicit:

```java
connection.setAutoCommit(false);   // stays at SERIALIZABLE — the CockroachDB default
```

The code deliberately never calls `setTransactionIsolation`, so what you measure is the engine's
default behavior, not something the PoC configured.

### 3.3 Decision 3 — a latch forces the one interleaving that exhibits the anomaly

Write skew appears only on **one** interleaving: both transactions read, *then* both write. Left to a
thread scheduler, the threads may run serially by accident — each would see the other's write, and the
anomaly would not appear, turning the test into a coin flip. The harness removes the randomness with a
`CountDownLatch(2)` barrier placed between each transaction's read and its write (deep-dived in
section 6).

> 📷 **Image prompt — code base / simulation diagram**
>
> ```text
> Generate a clean flat-vector architecture diagram titled "How the PoC
> simulates the write-skew race". Draw four horizontal module boxes: "core"
> (listing SignatoryGuard, SignatoryStore port, Tx interface), "adapter-mongo"
> (MongoSignatoryStore — snapshot isolation), "adapter-cockroach"
> (JdbcSignatoryStore — strict SERIALIZABLE), and "e2e" (Cucumber scenarios +
> latch harness). Above the modules, draw a harness box spawning two thread
> arrows labeled "tx-alice" and "tx-bob" that both pass through a barrier icon
> labeled "CountDownLatch(2): read-read, then write-write" and then call into a
> shared "SignatoryStore port". From the port, two arrows descend — one to a
> MongoDB cylinder and one to a CockroachDB cylinder — and annotate that only one
> adapter is wired per scenario, selected by a tag "@engine-mongo" or
> "@engine-cockroach". At the bottom, add an "evidence" lane showing three
> outputs: before/after committed rows, per-transaction trace, and a verdict
> table. Architecture-diagram aesthetic, clear sans-serif labels, one accent
> color, no code snippets, no logos.
> ```

---

## 4. The setup, in code and configuration

### 4.1 Build structure (condensed)

A Maven multi-module project. The parent pins versions and imports the JUnit BOM to align the platform
artifacts (Cucumber 7.34.8 pulls a newer `junit-platform-engine` than the suite API, which would
otherwise fail with a `NoSuchMethodError`):

```xml
<groupId>io.forest.isolation</groupId>
<artifactId>write-skew-poc</artifactId>
<packaging>pom</packaging>

<modules>
    <module>core</module>
    <module>adapter-mongo</module>
    <module>adapter-cockroach</module>
    <module>e2e</module>
</modules>

<properties>
    <java.version>21</java.version>
    <cucumber.version>7.34.8</cucumber.version>
    <junit-jupiter.version>5.13.4</junit-jupiter.version>
    <junit-platform.version>1.13.4</junit-platform.version>
    <mongodb-driver.version>5.2.1</mongodb-driver.version>
    <postgresql.version>42.7.4</postgresql.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>  <!-- aligns every org.junit.platform/jupiter artifact on one release -->
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>${junit-jupiter.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- raw drivers: mongodb-driver-sync, postgresql; plus cucumber + junit-platform-suite -->
    </dependencies>
</dependencyManagement>
```

The `e2e` module gates the whole suite behind a profile. The suite class is named `*IT`, and Surefire
only includes it when the profile is active — so a plain `mvn test` never needs Docker:

```xml
<profile>
    <id>e2e</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <includes><include>**/*IT.java</include></includes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

### 4.2 Containers

MongoDB needs a replica set before it will accept transactions, so the compose file starts one and an
init sidecar runs `rs.initiate` and waits for a writable primary:

```yaml
# docker-compose.mongo.yml (condensed)
name: write-skew-poc
services:
  mongo:
    image: mongo:7.0
    command: ["--replSet", "rs0", "--bind_ip_all"]
    ports: ["27017:27017"]

  mongo-init:                      # makes the single node a usable replica set
    image: mongo:7.0
    depends_on: [mongo]
    entrypoint: ["/bin/bash", "-c"]
    command:
      - |
        # poll 90 x 2s: ping the server, initiate the set, then wait for a primary
        mongosh --host mongo:27017 --quiet --eval \
          'try { rs.initiate({_id: "rs0", members: [{_id: 0, host: "localhost:27017"}]}) } catch (e) { print("rs already initiated") }'
        mongosh --host mongo:27017 --quiet --eval 'db.hello().isWritablePrimary' | grep -q true && exit 0
```

CockroachDB runs single-node with an init sidecar that applies the schema, so "ready" means "the schema
exists", not merely "the port answers":

```yaml
# docker-compose.cockroach.yml (condensed)
name: write-skew-poc
services:
  crdb:
    image: cockroachdb/cockroach:latest-v23.2
    command: start-single-node --insecure --store=/cockroach/cockroach-data
    ports: ["26257:26257", "8080:8080"]

  crdb-init:                       # applies the schema, then exits
    image: cockroachdb/cockroach:latest-v23.2
    depends_on: [crdb]
    volumes: ["./init-cockroach.sql:/scripts/init-cockroach.sql:ro"]
    entrypoint: ["/bin/bash", "-c"]
    command:
      - |
        # poll 90 x 2s: SELECT 1 until the node accepts SQL, then apply the schema
        ./cockroach sql --insecure --host=crdb:26257 -f /scripts/init-cockroach.sql
```

The CockroachDB schema seeds a deterministic start state and, notably, defines the `party_counter` row
that only the *mitigation* touches:

```sql
CREATE TABLE IF NOT EXISTS signatory (
    id         STRING PRIMARY KEY,
    party_id   STRING NOT NULL,
    authorized BOOL   NOT NULL
);

-- The hot row a snapshot-isolated engine needs to close the gap in application code.
CREATE TABLE IF NOT EXISTS party_counter (
    party_id STRING PRIMARY KEY,
    seq      INT    NOT NULL
);

DELETE FROM signatory;
INSERT INTO signatory (id, party_id, authorized) VALUES
    ('alice', 'P1', true),
    ('bob',   'P1', true);

DELETE FROM party_counter;
INSERT INTO party_counter (party_id, seq) VALUES ('P1', 0);
```

### 4.3 The scenarios, written in Gherkin

Each feature file carries a tag that selects the adapter. The scenarios are engine-agnostic; the tag is
the only thing that changes:

```gherkin
# mongo_snapshot_isolation.feature
@engine-mongo
Feature: Snapshot isolation permits write skew

  Scenario: concurrent resignations commit without conflict but break the invariant
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign as signatories
    Then both requests committed with no error
    # WRITE SKEW: no serial order of the two transactions could have produced this state
    And 0 authorized signatories remain

  Scenario: a hot-row counter closes the gap (application-level mitigation)
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign against a guarded party
    Then exactly one resigns
    And 1 authorized signatory remains
```

```gherkin
# cockroach_serializable.feature
@engine-cockroach
Feature: Strict serializable prevents write skew

  Scenario: concurrent resignations are serialised and the invariant holds
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign as signatories
    Then exactly one commits
    And the other fails with a serialization error (SQLSTATE 40001)
    # WRITE SKEW PREVENTED: the read-write dependency cycle was detected and one side aborted
    And 1 authorized signatory remains

  Scenario: a retry loop preserves the invariant and still succeeds
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign with retry-on-serialization
    Then exactly one resigns and one is refused on the retry
    And 1 authorized signatory remains
```

### 4.4 The suite entry point

One `@Suite` class bridges JUnit Platform to the Cucumber engine. It selects the Cucumber engine, the
glue package, and the report plugin. The package selector `features` is deliberate: the Cucumber
engine warns when feature files are discovered through a classpath-resource selector, and the package
selector finds the same two files without the warning.

```java
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.forest.isolation.signatory")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, json:target/cucumber-reports/cucumber.json, html:target/cucumber-reports/cucumber.html")
public class WriteSkewE2EIT {
}
```

The `PLUGIN_PROPERTY_NAME` value is what writes the per-scenario traces to
`target/cucumber-reports/cucumber.{json,html}` that section 9 refers to.

---

## 5. The implementation, in depth

### 5.1 The MongoDB adapter — snapshot isolation, used as-is

```java
public final class MongoSignatoryStore implements SignatoryStore {

    @Override
    public <T> T inTransaction(Function<Tx, T> action) {
        try (ClientSession session = client.startSession()) {
            session.startTransaction();
            try {
                T result = action.apply(new MongoTx(session, signatory(), counter()));
                session.commitTransaction();
                return result;
            } catch (RuntimeException e) {
                session.abortTransaction();
                throw e;
            }
        }
    }

    private static final class MongoTx implements Tx {
        public int countAuthorized(String partyId) {
            return (int) signatory.countDocuments(session,
                    Filters.and(Filters.eq("party_id", partyId), Filters.eq("authorized", true)));
        }
        public void setAuthorized(String partyId, String signatoryId, boolean authorized) {
            signatory.updateOne(session, Filters.eq("_id", signatoryId),
                    Updates.set("authorized", authorized));
        }
        public void touch(String partyId) {
            counter.updateOne(session, Filters.eq("_id", partyId), Updates.inc("seq", 1));
        }
    }
}
```

The isolation level is never raised or lowered here. MongoDB multi-document transactions are
snapshot-isolated, so disjoint writes to `alice` and `bob` produce no conflict, and both commit.

### 5.2 The CockroachDB adapter — strict serializable, plain JDBC

```java
public final class JdbcSignatoryStore implements SignatoryStore {

    @Override
    public <T> T inTransaction(Function<Tx, T> action) {
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);   // stays at SERIALIZABLE (engine default)
            try {
                T result = action.apply(new JdbcTx(connection));
                connection.commit();
                return result;
            } catch (Exception e) {
                rollback(connection);
                throw e instanceof RuntimeException runtime ? runtime : new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class JdbcTx implements Tx {
        public int countAuthorized(String partyId) {
            // SELECT count(*) FROM signatory WHERE party_id = ? AND authorized
        }
        public void setAuthorized(String partyId, String signatoryId, boolean authorized) {
            // UPDATE signatory SET authorized = ? WHERE id = ?
        }
        public void touch(String partyId) {
            // UPDATE party_counter SET seq = seq + 1 WHERE party_id = ?
        }
    }
}
```

(`JdbcTx`'s method bodies above are abbreviated to the SQL they issue; the transaction handling is
shown in full.)

CockroachDB's default is SERIALIZABLE. Because the dependency between the read (`count(*)`) and the
other transaction's write is tracked, one of the two transactions is aborted with `SQLSTATE 40001`
(`RETRY_SERIALIZABLE`) even though the two writes touch different rows.

### 5.3 The hot-row counter — the application-level mitigation

The guarded variant adds a `touch` before the check, reintroducing a serialization point that snapshot
isolation does not provide:

```java
public static boolean revokeGuarded(Tx tx, String partyId, String signatoryId) {
    tx.touch(partyId);          // write the shared counter row first
    return revoke(tx, partyId, signatoryId);
}
```

Now both resignations *write the same row* (`party_counter.seq`). That manufactures a write-write
conflict on a row both must touch, which snapshot isolation **can** detect. One transaction is aborted
with a `WriteConflict` (error `112`), and the counter advances exactly once.

---

## 6. The deterministic harness, in depth

The heart of the harness is a `CountDownLatch(2)` between the read and the write of each transaction:

```java
private static Outcome run(SignatoryStore store, boolean retry, boolean guarded) {
    CountDownLatch bothRead = new CountDownLatch(2);
    Attempt alice = new Attempt();
    Attempt bob = new Attempt();
    List<String> trace = new CopyOnWriteArrayList<>();

    trace.add("mode=" + (guarded ? "guarded" : retry ? "retry-on-serialization" : "plain")
            + " interleaving=read-read-then-write-write (latch-forced)");

    Thread first  = new Thread(() -> attempt(store, ALICE, bothRead, retry, guarded, alice, trace), "tx-alice");
    Thread second = new Thread(() -> attempt(store, BOB,   bothRead, retry, guarded, bob,   trace), "tx-bob");
    first.start();
    second.start();
    join(first);
    join(second);

    int finalAuthorized = store.inTransaction(tx -> tx.countAuthorized(PARTY_ID));   // read AFTER, outside the race
    trace.add("harness | VERIFY party=" + PARTY_ID + " authorized=" + finalAuthorized
            + " (minimum required=" + SignatoryGuard.MIN_AUTHORIZED + ")");
    return new Outcome(alice.committed, bob.committed, alice.revoked, bob.revoked,
            alice.error, bob.error, finalAuthorized, List.copyOf(trace));
}

private static void attempt(SignatoryStore store, String signatoryId, CountDownLatch bothRead,
                            boolean retry, boolean guarded, Attempt out, List<String> trace) {
    String who = Thread.currentThread().getName();
    try {
        out.revoked = store.inTransaction(tx -> {
            int authorized = tx.countAuthorized(PARTY_ID);       // READ — before either write
            trace.add(who + " | READ   party=" + PARTY_ID + " -> " + authorized + " authorized");
            awaitBarrier(bothRead);                              // wait for the other thread's read
            boolean revoked = guarded
                    ? SignatoryGuard.revokeGuarded(tx, PARTY_ID, signatoryId)
                    : SignatoryGuard.revoke(tx, PARTY_ID, signatoryId);
            trace.add(who + " | WRITE  signatory=" + signatoryId + " authorized=false"
                    + (revoked ? "" : " [refused: would break the invariant]"));
            return revoked;
        });
        out.committed = true;
        trace.add(who + " | COMMIT revoked=" + out.revoked);
    } catch (RuntimeException e) {
        trace.add(who + " | ABORT  " + describe(e));
        if (retry && isSerializationFailure(e)) {
            out.revoked = store.inTransaction(tx -> SignatoryGuard.revoke(tx, PARTY_ID, signatoryId));
            trace.add(who + " | RETRY  after SQLSTATE 40001 -> revoked=" + out.revoked);
        }
        out.error = e;
    }
}
```

Three things to notice:

- **The barrier makes the race deterministic.** `bothRead.await()` blocks each thread until the other
  has completed its read, forcing read-read-then-write-write on every run.
- **The harness fixes the *shape*, not the winner.** On a given run CockroachDB may abort `alice` or
  `bob`; the losing thread varies, but the end state (`authorized = 0` vs `1`) and the commit counts are
  stable. The assertions only ever depend on the *counts*, never on which thread lost.
- **The read-back is outside the race.** After both threads finish, the harness opens a fresh
  transaction and counts the committed rows. That is the authoritative end state, independent of what
  either transaction believed.

### 6.1 Why `Outcome` separates "committed" from "resigned"

The verdict table must distinguish "one transaction committed" from "one resignation actually took
effect", which `commits()` alone cannot express. Hence a record with four booleans plus the trace:

```java
public record Outcome(
        boolean aliceCommitted, boolean bobCommitted,
        boolean aliceRevoked,  boolean bobRevoked,
        Throwable aliceError,  Throwable bobError,
        int finalAuthorized, List<String> trace) {

    public int commits()      { return (aliceCommitted ? 1 : 0) + (bobCommitted ? 1 : 0); }
    public int resignations() { return (aliceRevoked ? 1 : 0) + (bobRevoked ? 1 : 0); }
}
```

This matters because in the retry scenario one transaction commits and the other is *refused on the
retry* — a `commits()` of 1 with a `resignations()` of 1, but for a subtle reason the trace makes
explicit.

### 6.2 How the evidence is captured

The step definitions attach every observable — before/after committed rows, the trace, the response, and
a verdict — to the Cucumber report, then roll each scenario up into one markdown table:

```java
@Then("^([0-9]+) authorized signator(?:y|ies) remain(?:s)?$")
public void authorizedSignatoriesRemain(int expected) {
    assertEquals(expected, outcome.finalAuthorized(), "unexpected number of authorized signatories");
}

@After
public void captureEvidence(Scenario scenario) {
    if (outcome == null) {
        return;                       // the scenario failed before it reached the concurrent step
    }
    String response = describeResponse(outcome);
    String verdict = verdict(outcome);
    scenario.log("AFTER    " + snapshot.get());
    scenario.log("RESPONSE " + response);
    scenario.log("VERDICT  " + verdict);
    EvidenceReport.record(engine, isolation, scenario.getName(), response,
            outcome.finalAuthorized(), verdict);
}

@AfterAll
public static void writeVerdictTable() {
    EvidenceReport.write();
}
```

`EvidenceReport` writes the consolidated table to `target/evidence/verdict.md` at the end of the suite,
so the proof travels with the build rather than living in console scrollback.

---

## 7. Results

The measured verdict table, end to end against real containers (MongoDB 7.0 replica set, CockroachDB
v23.2 — no mocks):

| Engine | Isolation | Concurrent resignations | Final `authorized` | Verdict |
| --- | --- | --- | --- | --- |
| MongoDB | snapshot | 2 / 2 committed, no error | **0** | WRITE SKEW — invariant silently broken |
| MongoDB + hot-row counter | snapshot + mitigation | 1 / 2 committed, `WriteConflict` (112) | **1** | Prevented, at the cost of a hot row |
| CockroachDB | strict serializable | 1 / 2 committed, other aborted `SQLSTATE 40001` | **1** | Prevented, no application coordination |
| CockroachDB + retry | serializable + retry | 1 committed, 1 aborted then refused on retry | **1** | Prevented, transient error absorbed |

The traces below are representative. The *shape* is fixed by the latch — both reads, then both
writes — but the order in which the two threads reach each milestone varies from run to run (and on
the serializable engine, so does which thread loses). What never varies is the end state and the
counts. The unmitigated MongoDB case first:

```text
mode=plain interleaving=read-read-then-write-write (latch-forced)
tx-bob   | READ   party=P1 -> 2 authorized
tx-alice | READ   party=P1 -> 2 authorized
tx-alice | WRITE  signatory=alice authorized=false
tx-bob   | WRITE  signatory=bob   authorized=false   <-- different documents: no conflict
tx-bob   | COMMIT revoked=true
tx-alice | COMMIT revoked=true
harness  | VERIFY party=P1 authorized=0 (minimum required=1)
```

The failure mode here is **silent**: the client receives two success responses, and the only record of
the breach is that `authorized` became `0`. Contrast with the CockroachDB trace, where the failure is
**loud**:

```text
mode=plain interleaving=read-read-then-write-write (latch-forced)
tx-alice | READ   party=P1 -> 2 authorized
tx-bob   | READ   party=P1 -> 2 authorized
tx-alice | WRITE  signatory=alice authorized=false
tx-bob   | ABORT  SQLSTATE 40001 (RETRY_SERIALIZABLE)
harness  | VERIFY party=P1 authorized=1 (minimum required=1)
```

And the hot-row guard, where the counter's single increment is the fingerprint of the mechanism:

```text
mode=guarded interleaving=read-read-then-write-write (latch-forced)
tx-alice | READ   party=P1 -> 2 authorized
tx-bob   | READ   party=P1 -> 2 authorized
tx-alice | WRITE  signatory=alice authorized=false
tx-alice | COMMIT revoked=true
tx-bob   | ABORT  MongoCommandException: error 112 (WriteConflict)
harness  | VERIFY party=P1 authorized=1 (minimum required=1)
```

`party_counter.seq` moves `0 → 1` — not `2` — because the losing transaction's increment is rolled back
with it. The counter advanced exactly once, and that single committed increment is read back from the
database *outside* any transaction, which is stronger evidence than a log line.

And the retry loop on the serializable engine, where the aborted transaction re-reads and is *refused*
rather than silently re-applied:

```text
mode=retry-on-serialization interleaving=read-read-then-write-write (latch-forced)
tx-alice | READ   party=P1 -> 2 authorized
tx-bob   | READ   party=P1 -> 2 authorized
tx-alice | WRITE  signatory=alice authorized=false
tx-bob   | WRITE  signatory=bob   authorized=false
tx-alice | COMMIT revoked=true
tx-bob   | ABORT  SQLSTATE=40001 (RETRY_SERIALIZABLE)
tx-bob   | RETRY  after SQLSTATE 40001 -> revoked=false   <-- re-read counts 1, guard refuses
harness  | VERIFY party=P1 authorized=1 (minimum required=1)
```

The `RETRY` line is the whole point of the mitigation: the re-run reads `1`, the guard refuses, and
the caller gets a deterministic business answer instead of a transient `40001`.

### 7.1 Reproducibility

The full suite was run multiple times on the same host with the same command, and the consolidated
`verdict.md` was byte-identical across runs. What is stable — end states, commit counts, and error
codes — is exactly what the assertions depend on. What is *not* stable is which thread loses, and no
assertion depends on that.

---

## 8. The mitigations, in depth

### 8.1 Mitigation 1 — the hot-row counter (engineer the conflict)

If snapshot isolation is the only option, you must reintroduce the serialization point the engine does
not provide. Every mutation first writes a shared `party_counter` row, so two concurrent resignations
race on the same row and one is aborted by a write-write conflict.

- **Correct** and cheap to add: a single `UPDATE … seq = seq + 1` inside the transaction.
- **The cost is a hot row.** Every resignation for a party contends on one document, serializing
  operations that snapshot isolation was otherwise happy to run in parallel.
- **It is a discipline, not a guarantee.** One forgotten code path that omits the `touch` reopens the
  hole, with no runtime signal.

> 📷 **Image prompt (optional) — hot-row counter mitigation diagram**
>
> ```text
> Generate a clean flat-vector diagram titled "The hot-row counter serializes
> concurrent resignations". Show two transactions "T1 (alice)" and "T2 (bob)"
> both writing the same shared row "party_counter.seq = 0 -> 1" before their own
> signatory row, under the heading "snapshot isolation". Depict the shared row as
> a hot spot with a glow, and show T2 being aborted with a "WriteConflict (112)"
> badge while T1 commits, leaving "authorized = 1". Use the same monochrome-plus-
> one-accent style as the other diagrams, no code, no logos.
> ```

### 8.2 Mitigation 2 — the serialization retry loop (absorb the error)

With a serializable engine, the work is *absorption*, not prevention. Catch the `SQLSTATE 40001`,
re-read, and re-attempt. On the retry the transaction sees the committed resignation, reads `1`, and
refuses — deterministically.

The harness detects the failure by walking the exception chain for the SQLSTATE:

```java
static boolean isSerializationFailure(Throwable error) {
    return "40001".equals(sqlState(error));
}
```

The two mitigations are not interchangeable: the first *prevents* the anomaly on an engine that cannot
detect it, the second *absorbs* the aborts an engine correctly raises.

---

## 9. Reproduce it

Prerequisites: JDK 21+, Maven 3.9+, and a running Docker daemon (the engines run in containers).

```bash
# Both engines, all four scenarios
mvn -pl e2e -am -Pe2e test

# One engine only (verdict table contains only that engine's rows)
mvn -pl e2e -am -Pe2e -Dcucumber.filter.tags=@engine-mongo test
mvn -pl e2e -am -Pe2e -Dcucumber.filter.tags=@engine-cockroach test

# Plain build: no Docker, no e2e suite (the suite class ends in *IT)
mvn test
```

Each run regenerates `target/evidence/verdict.md` (the consolidated table) and
`target/cucumber-reports/cucumber.{json,html}` (per-scenario traces). Containers and volumes are torn
down automatically at the end of the run.

---

## 10. Finalization notes

1. **Diagrams** — run the four prompts through Gemini and drop the images at the `📷` markers: the
   overview infographic at the top of the article (directly above "In this article"), the concept
   diagram under section 2, the architecture diagram under section 3, and (optional) the mitigation
   diagram under section 8.1.
2. **Code excerpts** — a few are condensed (`# … condensed` / `// …`); they are faithful to the source,
   and the article no longer depends on the reader having the repository.
3. **Trim to length** — sections 5–6 can be cut for a shorter form without losing the argument: the
   invariant (section 1), the four conditions (section 2), and the verdict table (section 7) are the
   load-bearing pieces.

