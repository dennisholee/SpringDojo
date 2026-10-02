# LTAP Lakehouse Isolation PoC

> **Status (2026):** this repository is now being developed as a **customer-onboarding** solution built
> on the isolation verdict below — see [§0](#0-status-from-an-isolation-poc-to-a-customer-onboarding-solution).
> The isolation PoC itself is unchanged and remains the evidence base.

A self-contained proof of concept that carries the thesis of
[`48_WriteSkewIsolation`](../48_WriteSkewIsolation) onto a **third** engine: object storage. It
exists to drive an architecture decision: *when the analytical copy of the data lives in an Apache
Iceberg lakehouse on S3-compatible storage, what transactional guarantees do we actually get — and
where do they stop?*

## 0. Status: from an isolation PoC to a customer-onboarding solution

The isolation question is answered below and stays answered. What changed is the **use case** the
answer is applied to.

| | Before | Now |
| --- | --- | --- |
| Question | *Does a lakehouse give serializability?* | *Can a lakehouse hold the write side for customer onboarding — transactionally?* |
| Entities | one `signatory` table + a `party_counter` | `party`, `partyRelationship`, `contactPoint` |
| Invariants | one (a party keeps one authorized signatory) | three: a primary contact point (count/absence), referential integrity (cross-table), uniqueness (phantom/duplicate) |
| Role of the verdict | the headline | the **design input**: every invariant is stated with the anomaly class it belongs to and the mechanism that protects it |
| Write path | a seeded fixture | a real onboarding submission, one transaction, later a CDC path |

The new domain core is in `core/src/main/java/io/forest/lakehouse/onboarding` and its invariants are
pinned down by tests that need no container:

```bash
mvn -pl core test             # 14 tests, incl. a reproducible onboarding write-skew scenario and its mitigation
mvn -pl service -am test      # 22 tests: module boundaries, idempotency, the submission API (no Docker)
mvn -pl e2e -am -Pe2e test    # Docker: the adapter over a real catalog, plus the retained isolation PoC
```

Planning lives in [`specs/003-customer-onboarding`](../specs/003-customer-onboarding) (`spec.md`,
`plan.md`, `tasks.md`); the decisions in
[ADR-007](docs/adr/ADR-007-reframe-signatory-as-customer-onboarding.md) (the reframe),
[ADR-008](docs/adr/ADR-008-modulith-with-hexagonal-edges.md) (modulith with hexagonal edges, and where
Spring is allowed), [ADR-009](docs/adr/ADR-009-cross-entity-atomicity-and-idempotency.md) (cross-table
atomicity via the catalog's multi-table commit, idempotency at the edge) and
[ADR-010](docs/adr/ADR-010-transactional-architecture-and-modern-stack.md) (transaction placement and
the 2026 stack). The `signatory` code below is kept: it is the reproducible evidence the reframe cites.

The persistence adapter for the new use case is implemented and verified against a real catalog and real
object storage: `IcebergOnboardingStore` publishes one unit of work as one catalog action
(`TableCommit` + `RESTCatalog#commitTransaction`), so a writer that dies mid-onboarding leaves nothing
visible. See [`specs/003-customer-onboarding/tasks.md`](../specs/003-customer-onboarding/tasks.md)
(T003-004, T003-005) for the evidence, and ADR-009 for how the mechanism turned out to differ from what
the ADR first assumed.

Phase 3 is done as well: `ltap-service` exposes `POST /onboarding-submissions` behind an
`Idempotency-Key`, with Spring Modulith boundaries (ADR-008) and every refusal an explicit code — a
replay returns the first response verbatim, and a `409` only ever comes from a documented conflict
(`OnboardingRefusal` for the domain's, `OnboardingApiExceptionHandler` for the protocol's). Two
limitations are recorded rather than hidden: the idempotency record is **in-process**, so a retry that
crosses a restart is caught by INV-3 (a duplicate tax id) rather than replayed; and because a
`TableCommit` asserts each table's snapshot, *any* concurrent write to the onboarding tables refuses the
whole transaction (ADR-009).

A close-out read of Phases 1–4 against the thesis — which rows are **measured**, which are **reasoned**,
and what a skeptic would attack — is recorded in [`docs/ASSESSMENT.md`](docs/ASSESSMENT.md), together
with a gap → effort → value ordering of what remains. [`docs/DESIGN.md`](docs/DESIGN.md) is written in the
same two layers: §1–§8 the isolation PoC exactly as measured, §9–§10 the onboarding design built on it,
§11–§12 the evidence model and the build.

## 1. Purpose

Run the *same* concurrent operation, through the *same* invariant and the *same* harness, against a
lakehouse:

| Engine | Isolation | Observed outcome |
| --- | --- | --- |
| Apache Iceberg on MinIO (object storage) | snapshot, optimistic commits | **write skew happens**: two disjoint revocations both commit, invariant silently broken |
| The same table, with a compare-and-set counter write in front of the rule | snapshot + application-level serialization point | **write skew prevented**: the losing commit is refused by the catalog |

The invariant is unchanged from #48 — a customer-data-master governance rule: **a party (customer)
must always keep at least one authorized signatory.** It is a *count/absence* constraint over a
party's signatories, so no unique index and no partition key can protect it.

## 2. The phenomenon, in one paragraph

Two transactions each read "party P1 has 2 authorized signatories", then each revoke *their own*
signatory. They write disjoint rows, so nothing in either commit names data the other one touched.
Iceberg's optimistic concurrency has nothing to detect, both commits land, and the table ends with
`0` authorized signatories — a state no serial order of the two transactions could have produced.
This is the same anomaly MongoDB exhibits in #48, and the point of the PoC is that **changing the
storage engine, and getting real ACID on object storage, does not change it.**

## 3. What the lakehouse *does* add

The engineered comparison is deliberately two-sided. Iceberg earns its place by adding the half that
neither engine in #48 offers, all on the *same* table the write-skew scenario corrupts:

| Capability | Proof |
| --- | --- |
| Atomic commits on object storage | concurrent writers on one partition: one commits, the other is refused by the catalog |
| Time travel | the pre-revocation snapshot still reads `2` authorized signatories after the skew broke the current one |
| Schema evolution | a column is added without rewriting a single Parquet file; old snapshots still read |
| Analytical scan | one projected, filtered scan answers "authorized signatories by party" across the whole table |

That is the LTAP shape: one physical copy of the rows, transactional enough to write to, analytical
enough to reason over, and versioned enough to audit.

## 4. Scope and non-goals

- **In scope:** the commit protocol's isolation behaviour, plus the analytical capabilities that
  justify a lakehouse. One shared rule, one deterministic harness, real object storage, real catalog.
- **Theme:** continues the CDM domain (same `party` / `signatory` concepts, same invariant) so the
  three engines in #42 (Dremio), #47 (CockroachDB) and #48 (MongoDB/CockroachDB) are directly
  comparable.
- **Out of scope (for this PoC):** cross-table atomicity (Iceberg commits are per table — see
  [ADR-006](docs/adr/ADR-006-what-this-does-not-prove.md)), distributed query engines, compaction,
  clustering, credential management, and any production hardening. Two of these are now *in scope for
  the onboarding feature* on top of the same engine: cross-table atomicity (ADR-009) and compaction
  (ADR-010); the PoC itself is left exactly as measured, so its verdict stays attributable.
- **Not a production reference:** insecure, single-node, `minioadmin`/`minioadmin`, local PoC use
  only.

## 5. Repository layout (Maven multi-module)

```
49_LTAP/
├── pom.xml                          # aggregator (packaging=pom) + dependencyManagement
├── README.md                        # this file
├── docker/
│   └── docker-compose.iceberg.yml       # MinIO (object storage) + Iceberg REST catalog
├── docs/
│   ├── DESIGN.md                        # two layers: the isolation finding, then the onboarding design
│   ├── ASSESSMENT.md                    # evidence class per claim + gap → effort → value
│   └── adr/ADR-00*.md                   # ten decision records
├── core/                            # engine-agnostic invariants + ports (JDK only; JUnit test-scope)
│   ├── src/main/java/io/forest/lakehouse/signatory/   # the isolation PoC (kept as the evidence)
│   │   ├── SignatoryGuard.java          # the rule, once
│   │   ├── SignatoryStore.java          # outbound port (inTransaction)
│   │   ├── Tx.java                      # read/write surface (countAuthorized/setAuthorized/touch)
│   │   ├── Lakehouse.java               # analytical port (time travel/schema/scan)
│   │   └── ColumnType.java
│   ├── src/main/java/io/forest/lakehouse/onboarding/   # the onboarding solution
│   │   ├── OnboardingService.java       # party + relationship + contact points, one transaction
│   │   ├── OnboardingPolicy.java        # INV-1 primary contact point; INV-3 uniqueness
│   │   ├── OnboardingStore.java         # outbound port (inTransaction + read side)
│   │   ├── OnboardingTx.java            # transactional read/write surface
│   │   └── OnboardingResult.java        # Onboarded | Rejected(code, detail)
│   └── src/test/java/io/forest/lakehouse/onboarding/   # in-memory harness + unit tests (no Docker)
├── adapter-iceberg/                 # the ports over the raw Apache Iceberg Java API
│   └── src/main/java/io/forest/lakehouse/
│       ├── signatory/IcebergSignatoryStore.java    # the isolation PoC's store (kept as evidence)
│       └── onboarding/IcebergOnboardingStore.java  # one unit of work -> one catalog commit
├── service/                         # the deployable: HTTP edge + idempotency (ADR-008)
│   ├── src/main/java/io/forest/lakehouse/service/
│   │   ├── LtapServiceApplication.java            # composition root: binds the ports to the adapter
│   │   ├── LtapCatalogProperties.java             # the lakehouse's coordinates, in this project's names
│   │   ├── IcebergPersistenceConfiguration.java   # the only place that names a store implementation
│   │   ├── Wire.java / MalformedSubmissionException.java      # text -> vocabulary, with stable codes
│   │   └── {party,relationship,contactpoint,onboarding}/      # the four Modulith modules
│   └── src/main/resources/application.yml
└── e2e/                             # real containers, no mocks: Cucumber scenarios + JUnit ITs
    └── src/test/...
        ├── e2e/Compose.java             # docker compose v2 wrapper
        ├── e2e/Warehouse.java           # boots MinIO + REST catalog, creates the bucket over S3
        ├── signatory/                   # the retained isolation evidence
        │   ├── IcebergFixture.java
        │   ├── ConcurrentRevoke.java    # latch harness: forces read-read-then-write-write
        │   ├── Outcome.java / EvidenceReport.java
        │   ├── LtapSteps.java / LtapE2EIT.java
        │   └── resources/features/lakehouse_isolation.feature
        └── onboarding/                  # the onboarding evidence
            ├── OnboardingFixture.java
            ├── OnboardingOnIcebergIT.java
            └── OrderedCommitWriter.java # the rejected two-commit design, kept as a comparator
```

## 6. Module dependency wiring

| Module | Packaging | Dependencies |
| --- | --- | --- |
| `core` | jar | none (plain Java). Test scope: `junit-jupiter`, `archunit` — the `CoreDependencyBoundaryTest` gate that fails the build if `io.forest.lakehouse.onboarding` reaches for anything but the JDK |
| `adapter-iceberg` | jar | `core`, `iceberg-core`, `iceberg-data`, `iceberg-parquet`, `iceberg-aws` + `iceberg-aws-bundle` (S3FileIO + AWS SDK), `parquet-column`/`parquet-hadoop`, `hadoop-client-api`/`hadoop-client-runtime` (runtime only) |
| `ltap-service` | jar | `core`, `adapter-iceberg`, `spring-boot-starter-web`, `spring-modulith-starter-core` (+ `spring-boot-starter-test`, `spring-modulith-starter-test`, and `core:tests` — `test` scope) |
| `e2e` | jar | `core` (+ `core:tests` — the reference engine of the onboarding rows — `test` scope), `adapter-iceberg` (test), cucumber (java + junit-platform-engine + picocontainer), junit-platform-suite, slf4j-simple — `test` scope |

No Spark and no Flink. **No Spring either — except in `ltap-service`**, and there only at the edges:
`core` is still plain Java, so ADR-004 is narrowed rather than reversed (ADR-008). The engine under test
remains the **table format plus its catalog**, driven through Iceberg's own Java API so the commit
behaviour stays reviewable; the service adds a transport edge and a composition root, and
`ServiceModuleStructureTest` fails the build if a framework type reaches the domain or a module boundary
is crossed.

## 7. The proof matrix

Two feature files, one suite, one verdict table.

Signatory (`lakehouse_isolation.feature`, tagged `@signatory @engine-iceberg`) — the retained isolation
PoC, and the finding this project is built on:

| # | Scenario | What it proves |
| --- | --- | --- |
| L1 | concurrent writers on one partition are refused, not merged | atomic commit + real conflict detection on object storage; no lost update |
| L2 | disjoint revocations both commit and break the invariant | **write skew** under snapshot isolation, exactly as MongoDB |
| L3 | a compare-and-set counter closes the gap | the application-level mitigation, and what it costs |
| L4 | the previous snapshot still holds the invariant | time travel: the corrupted state is recoverable and auditable |
| L5 | a column is added without rewriting a data file | schema evolution is metadata-only |
| L6 | one scan answers the governance question across parties | the analytical half of LTAP |

Onboarding (`onboarding_invariants.feature`, tagged `@onboarding`) — the same interleaving over the
onboarding aggregate, run against **two engines through one port**:

| # | Scenario | What it proves |
| --- | --- | --- |
| O1 | two concurrent demotions of different primary contact points both commit | **write skew**: 2 primaries in, 0 out, both commits reported success — recorded on `core`'s in-memory store, because an engine that refuses everything cannot show what is being protected (`@engine-reference`) |
| O2 | the serialization point closes the gap | the counter guard refuses the losing commit on an engine that only detects touched keys (`@engine-reference`) |
| O3 | the same interleaving against the lakehouse's cross-table commit | the catalog refuses the loser: the commit asserts the `contact_point` snapshot it read (`@engine-iceberg`) |
| O4 | the serialization point on the lakehouse | the guard adds nothing here — measured, not assumed (`@engine-iceberg`) |
| O5 | the refused demotion retried with a fresh read is declined by the rule | the `409` retry path: the fresh read declines the demotion itself and publishes nothing (`@engine-iceberg`) |
| O6 | the two commits are allowed to overlap | the row that *records* what the catalog does with the race instead of asserting an outcome — finding 6 in §12 (`@engine-iceberg`) |

## 8. Observed results

Produced by `mvn -pl e2e -am -Pe2e test` (also written to `e2e/target/evidence/verdict.md`). Long
expected/actual snapshot ids are elided; everything else is verbatim.

| Scenario | Result | Evidence | Verdict |
| --- | --- | --- | --- |
| L1 concurrent writers on one partition | 1/2 committed, `CommitFailedException: branch main has changed` | final authorized=1, counter=1 | PREVENTED — the catalog refused the losing commit, so no update was lost |
| L2 disjoint revocations | 2/2 committed | final authorized=0, counter=0 | **WRITE SKEW** — invariant broken |
| L3 compare-and-set counter | 1/2 committed, `CommitFailedException: branch main has changed` | final authorized=1, counter=1 | PREVENTED — no update was lost |
| L4 previous snapshot | 2/2 committed | final authorized=0, `snapshot <before>=2` | **WRITE SKEW** on the current snapshot, 2 authorized on the remembered one |
| L5 column added | metadata-only commit | `columns=[id, party_id, authorized, tier]`, dataFiles=2 | EVOLVED — no rewrite |
| L6 scan by party | one projected scan | `authorizedByParty={P1=2, P2=1}` | ANSWERED |

### Onboarding rows

The two reference rows need no container, so they reproduce anywhere:

```bash
mvn -pl e2e -am -Pe2e test -Dtest=LtapE2EIT -Dsurefire.failIfNoSpecifiedTests=false \
    -Dcucumber.filter.tags="@engine-reference"
```

| Engine | Scenario | Result | Evidence | Verdict |
| --- | --- | --- | --- | --- |
| in-memory reference harness | two concurrent demotions of different primary contact points both commit | 2/2 committed | final primary contact points=0, counter=1 | **WRITE SKEW** — INV-1 broken (0 left, both transactions committed) |
| in-memory reference harness | the serialization point closes the gap | 1/2 committed, abort=`ConcurrentCommitException: commit refused: counter[P1] moved since the transaction's snapshot` | final primary contact points=1, counter=2 | PREVENTED — the serialization point refused the losing commit |

The four `@engine-iceberg` onboarding rows (O3–O6) need the containers. The 2026-09-27 full run
(`mvn -pl e2e -am -Pe2e test`, 31 tests, 0 failures) re-ran the whole table after the commit-order
change, so the three sequenced lakehouse rows (O3–O5) are measured: each refused the losing commit, by a
different mechanism every time. The reason the earlier run had them *failing* their "exactly one commits"
assertion is finding 6 below — the harness still let the two commits overlap, and O6 now names that shape
instead of hiding it, which is why O6's row reads `LOST UPDATE` rather than `PREVENTED`. Read O6 as a
**recording**: it pins only the invariant, so it stays green whether the catalog refuses the loser or
accepts both.

## 9. Build and run

```bash
# The whole suite: boots MinIO + the REST catalog, runs all twelve scenarios, tears down
mvn -pl e2e -am -Pe2e test

# Filter scenarios by tag (the tag filter is understood by the Cucumber engine in every module)
mvn -pl e2e -am -Pe2e test -Dcucumber.filter.tags="@engine-iceberg"

# Only the rows that run on core's in-memory harness: no container has to boot. -Dtest keeps the
# JUnit half of the suite (which does need containers) out of the way.
mvn -pl e2e -am -Pe2e test -Dtest=LtapE2EIT -Dsurefire.failIfNoSpecifiedTests=false \
    -Dcucumber.filter.tags="@engine-reference"

# Plain build: no Docker, no e2e suite (the suite is *IT, included only by the e2e profile)
mvn test

# The HTTP edge. Its tests run against core's own in-memory harness, so they need no container.
mvn -pl service -am test
mvn -pl service -am -DskipTests package   # -> a runnable jar (Iceberg + Spring Boot inside)
```

`-am` is required so `core` and `adapter-iceberg` are resolved from the reactor. Useful overrides:

| Property | Effect |
| --- | --- |
| `-Dltap.compose.dir=<dir>` | look for `docker-compose.iceberg.yml` somewhere other than `../docker` |
| `-Dltap.compose.manage=false` | leave an already-running stack alone (how you attach and read container logs) |

### Submitting a customer

```bash
docker compose -f docker/docker-compose.iceberg.yml up -d
mvn -pl service -am -DskipTests package
java -jar service/target/ltap-service-0.1.0-SNAPSHOT.jar

curl -sS -X POST http://localhost:8080/onboarding-submissions \
  -H 'Idempotency-Key: 8f14e45f-ceea-467f-a5f8-1e2b3c4d5e6f' \
  -H 'Content-Type: application/json' \
  -d '{
        "party":         {"partyId":"P-100","type":"INDIVIDUAL","legalName":"Ada Lovelace",
                          "taxId":"TAX-100","kycLevel":"TIER_1"},
        "relationship":  {"relationshipId":"R-100","type":"RETAIL_CLIENT","status":"ACTIVE",
                          "effectiveFrom":"2026-09-27"},
        "contactPoints": [{"contactPointId":"C-100","channel":"EMAIL",
                           "value":"ada@example.com","primary":true}]
      }'
```

Run that command twice. The second response is the first one byte for byte and carries
`X-Idempotency-Status: replay`: a duplicated `POST` is answered, not re-executed, and no second party
appears. `409` comes back for a key reused with a *different* payload, for a tax id another party already
holds, and for a commit refused by a concurrent writer — the one refusal that is deliberately *not*
recorded, so that the client's retry can actually retry. `422` is for a submission that is well formed
but internally inconsistent (no contact point, or none marked primary). Startup fails if the catalog is
unreachable, on purpose: an edge that accepts a submission it cannot commit is worse than one that never
came up (ADR-009).

**Note on the stack.** `apache/iceberg-rest-fixture` is the reference REST catalog; the suite overrides
two of its defaults, both of which matter for a *concurrent* PoC and both of which are commented in
`docker-compose.iceberg.yml`:

- `CATALOG_URI` — the image defaults to `jdbc:sqlite::memory:`, which hands **each pooled connection
  its own empty database**. That is fine single-threaded and fails the moment two commits race, with
  `no such table: iceberg_tables`. The suite pins one real SQLite file so every connection shares the
  catalog.
- `CATALOG_JDBC_STRICT__MODE=false` — restores the standard one-`iceberg_tables`-row shape.

MinIO withdrew its official Docker Hub images during 2025, so the Compose file uses the same MinIO
server republished by Elestio; point it at your own registry mirror if you prefer.

## 10. Evidence artifacts

Each run writes, per scenario, the committed rows before and after the concurrency, the
per-transaction trace, the response and the verdict — into the Cucumber report and a consolidated
table:

| Artifact | Contents |
| --- | --- |
| `e2e/target/evidence/verdict.md` | consolidated verdict table for the run |
| `e2e/target/cucumber-reports/cucumber.html` | human-readable report, evidence attached to each scenario |
| `e2e/target/cucumber-reports/cucumber.json` | machine-readable equivalent |

## 11. Key versions

- Java 21 (`maven-compiler-plugin` `release=21`), Maven 3.9+
- Apache Iceberg **1.10.1**, raw Java API (`iceberg-core` / `iceberg-data` / `iceberg-parquet`).
  Raised from 1.7.1 for one reason: cross-table atomicity — `TableCommit` +
  `RESTCatalog#commitTransaction(...)` is the only way to publish several tables in one catalog action
  (ADR-009). The client types and the `POST /v1/{prefix}/transactions/commit` path already exist in the
  1.7.1 client, so no release boundary is claimed for them — 1.10.1 is the pairing that was actually
  exercised, because the server is the side that has to serve the endpoint. Pinned there rather than to
  the newest release because the reference REST-catalog server image tops out there.
- `iceberg-aws` + `iceberg-aws-bundle` — `S3FileIO` and the shaded AWS SDK it links against
- Parquet 1.16.0 (named explicitly: Iceberg consumes Parquet at `runtime` scope)
- Hadoop client API/runtime 3.4.1 (`runtime` scope only — Iceberg's Parquet writer holds a
  `Configuration` internally, even when every byte goes to S3)
- JUnit Platform + Cucumber 7
- Spring Boot **3.5.16** + Spring Modulith **1.4.13**, in `ltap-service` only. Both BOMs are imported at
  the *end* of the aggregator's `dependencyManagement`, so the JUnit 5.13.4 pin cucumber needs still
  wins: an import placed above `junit-bom` would silently re-version the whole reactor. A
  `spring-boot-dependencies` import brings dependency management but not plugin management, so the Boot
  plugin is pinned explicitly in `pluginManagement`.
- Docker with the Compose v2 plugin

## 12. Findings that shaped the design

Recording these matters more than the green build, because they are the things a lakehouse team
actually gets wrong:

1. **A lakehouse's "ACID" is not serializability.** Iceberg gives atomic snapshot commits; it does
   *not* give range/predicate protection, so a count/absence invariant is still exposed to write skew.
   The verdict table shows the same `WRITE SKEW` as MongoDB in #48 — on object storage.
2. **Concurrent partition overwrite converges; it does not conflict.** `ReplacePartitions` — and
   `OverwriteFiles` on a partition filter — retry internally and are last-writer-wins. The PoC first
   demonstrated this as a **silently lost counter increment** (`seq=1` after two committed increments,
   plus a duplicated row file) before the mitigation was changed.
3. **Conflict detection has to be asked for.** The mitigation only works because the counter write
   (a) commits a *delete of the exact file it read* and (b) runs on a table with
   `commit.retry.num-retries=0`, so a moved base snapshot becomes a refused commit instead of a
   transparent re-apply. Retry is a *policy*, and it decides whether you get an abort or a lost update.
4. **Iceberg's generic reader (`IcebergGenerics`) returned zero rows** for this table while the files
   read back correctly. The adapter consequently reads snapshots through an explicit
   `TableScan.project(...).filter(...)` + Parquet reader, which is also the API Iceberg recommends —
   and it makes the projection, the predicate and the pinned snapshot visible in the code.
5. **Cross-table atomicity is not provided.** The counter lives in its own table, so the transaction
   publishes two commits and orders them so that the compare-and-set goes first. That ordering is an
   application concern, not an engine guarantee (ADR-006). The onboarding increment removed that gap for
   this use case (ADR-009): the catalog's multi-table commit publishes all four tables in one action.
6. **A refused commit is a property of the *interleaving*, not of the engine.** A `TableCommit` asserts
   the base snapshot each transaction read, so a *sequenced* loser is refused — but when the two
   `commitTransaction` calls were in flight at once, the 1.10.1 reference catalog accepted **both** and one
   demotion vanished silently, `party_counter` increment with it. The invariant survived only because each
   snapshot is a full replacement built from the same base. So a `201` means "the use case ran and the
   commit was accepted", which is *not* always the same as "the write is in the table". The onboarding
   suite therefore names the commit order explicitly and records the overlapping case instead of asserting
   it (F-4, ADR-009).
   Root cause, measured (2026-09-27): the **catalog's** multi-table commit is not atomic across
   concurrent requests — the requirement check and the metadata write are two steps, so two transactions
   asserting the same base snapshot can both pass. It is not a client artifact: `RESTCatalog.loadTable`
   has no cache (a distinct `BaseTable` per call), and the lost update reproduces with two independent
   clients at the same rate. Over 10 attempts each: sequenced **0/10** lost updates (the loser is refused
   every time), overlapping **8/10** on one store, **7/10** across two independent clients.

## 13. Decisions recorded as ADRs

| ADR | Decision |
| --- | --- |
| [ADR-001](docs/adr/ADR-001-lakehouse-as-third-engine.md) | Apache Iceberg on MinIO as the third engine |
| [ADR-002](docs/adr/ADR-002-engine-agnostic-core.md) | Reuse #48's ports-and-adapters core, extended with a `Lakehouse` port |
| [ADR-003](docs/adr/ADR-003-partition-and-retry-are-the-serialization-point.md) | Partition layout and commit retry are the serialization point |
| [ADR-004](docs/adr/ADR-004-raw-iceberg-java-api.md) | Raw Iceberg Java API: no Spark, no Flink, no Spring |
| [ADR-005](docs/adr/ADR-005-rest-catalog-and-s3fileio.md) | REST catalog for the atomic snapshot decision; S3FileIO for object storage |
| [ADR-006](docs/adr/ADR-006-what-this-does-not-prove.md) | What this PoC does not prove |
| [ADR-007](docs/adr/ADR-007-reframe-signatory-as-customer-onboarding.md) | Reframe the use case: signatory revocation → customer onboarding |
| [ADR-008](docs/adr/ADR-008-modulith-with-hexagonal-edges.md) | Modulith with hexagonal edges; Spring at the edges only (narrows ADR-004) |
| [ADR-009](docs/adr/ADR-009-cross-entity-atomicity-and-idempotency.md) | Cross-table atomicity via the catalog's multi-table commit (Nessie is its production home); idempotency at the edge |
| [ADR-010](docs/adr/ADR-010-transactional-architecture-and-modern-stack.md) | OLTP-front+CDC vs direct-write lakehouse, and the 2026 stack baseline |


