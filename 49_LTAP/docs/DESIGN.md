# Design — LTAP Lakehouse, from an Isolation Finding to Customer Onboarding

> **Two layers, one document.** §1–§8 describe the **isolation PoC** exactly as measured on the
> `signatory` table. §9–§10 describe the **customer onboarding** solution built on that finding, and
> §11–§12 apply to both. The finding is the *design input*, not history: every invariant in §9 is stated
> with the anomaly class it belongs to and with the mechanism §7–§8 showed can protect it. Planning lives
> in [`specs/003-customer-onboarding`](../../specs/003-customer-onboarding); the decisions in ADR-007 …
> ADR-010; the evidence class of every claim is graded in [`ASSESSMENT.md`](ASSESSMENT.md).

## 1. Goal

Answer two questions with evidence, in order.

**First — the finding (§1–§8).** *When the transactional copy of the data lives in an Apache Iceberg
lakehouse on S3-compatible object storage, what does "transactional" actually buy us?* The PoC takes the
invariant, the concurrency harness and the step vocabulary of
[`48_WriteSkewIsolation`](../48_WriteSkewIsolation) — unchanged — and points them at a third engine. If
the write-skew verdict moves, the engine moved it; if it does not, the anomaly is a property of snapshot
isolation rather than of any particular database.

**Then — the solution (§9–§10).** *Can that engine hold the write side for customer onboarding,
transactionally?* The finding says a partition-scoped commit cannot close a **cross-entity** invariant, so
the solution moves the unit of work up to the catalog: one onboarding submission is published as one
**multi-table commit** (§7) behind one HTTP edge (§10).

## 2. Principles

1. **One rule, three engines.** `SignatoryGuard` is the only place the invariant exists. It has no
   database, table-format or framework dependency.
2. **The engine is the only variable.** The harness forces exactly one interleaving (read-read, then
   write-write) with a `CountDownLatch`, so the result is reproducible rather than a flaky race.
3. **Evidence, not expectation.** Every verdict is derived from the observed end state and includes
   the per-transaction trace. A regression changes the verdict table, not a comment.
4. **Real everything.** Real object storage, a real catalog, real Parquet files, real commits. No
   in-memory table format, no mocked FileIO.
5. **The costs are visible.** Every awkward dependency (Parquet's `runtime` scope, Hadoop's
   `Configuration`, the catalog's two non-default settings) is named in a comment or an ADR rather
   than absorbed silently.
6. **One port, two engines, one table.** The onboarding invariants are driven by *one* latch harness
   through *one* port against both an in-memory reference store and the lakehouse, so the verdict table
   compares engines instead of asserting one of them. A table containing only the lakehouse could not
   tell "safe by design" from "this engine happens to refuse everything" (§11).
7. **An invariant names its anomaly class.** INV-1 is a count/absence predicate, INV-2 a cross-table
   reference, INV-3 a uniqueness/phantom constraint. Each is protected by a different mechanism, and each
   is stated together with the one it uses (§9).

## 3. System view

```text
                       ┌──────────────────────── e2e (test scope) ───────────────────────┐
                       │  LtapE2EIT ──> LtapSteps ──> ConcurrentRevoke (latch harness)   │
                       │                    │                     │                      │
                       │              IcebergFixture              │                      │
                       └────────────────────┼─────────────────────┼──────────────────────┘
                                            │                     │
                    core: SignatoryGuard ───┘                     │
                          SignatoryStore (port)  <───────────────┘
                          Tx / Lakehouse (ports)
                                            │
                       adapter-iceberg: IcebergSignatoryStore (implements both ports)
                                            │
                       RESTCatalog ──HTTP──> iceberg-rest ──JDBC──> SQLite (iceberg_tables)
                       S3FileIO    ──S3───> MinIO (bucket: warehouse/)
```

The onboarding solution reuses that stack and adds exactly one layer above it. The difference that matters
is in the arrow that leaves the adapter: not two `commit`s in a chosen order, but one call carrying every
table's commit.

```text
   HTTP  POST /onboarding-submissions  (Idempotency-Key)
     │
     ▼
   service  ── OnboardingController ── modules: party | relationship | contactpoint | onboarding
     │
     ▼
   core     ── OnboardingService.onboard → OnboardingPolicy (INV-1 / INV-2 / INV-3)
     │         ports: OnboardingStore / OnboardingTx / OnboardingLakehouse
     ▼
   adapter-iceberg ── IcebergOnboardingStore
     │
     ├── RESTCatalog ──HTTP──> iceberg-rest   (one POST /v1/{prefix}/transactions/commit)
     └── S3FileIO    ──S3───> MinIO (bucket: warehouse/)
```

The `e2e` module drives both layers through the same vocabulary: `ConcurrentRevoke` for the finding and
`ConcurrentDemote` for the solution are both port-only latch harnesses, the direct analogues of each other.

## 4. Modules and dependency direction

| Module | Depends on | Purpose |
| --- | --- | --- |
| `core` | nothing but the JDK (JUnit test-scope) | both rule sets and their ports — `signatory` (the finding) and `onboarding` (the solution) — plus the framework-free `OnboardingLakehouse` read port |
| `adapter-iceberg` | `core`, Iceberg, Parquet, AWS SDK, Hadoop (runtime) | both ports over the raw Iceberg Java API: `IcebergSignatoryStore` and `IcebergOnboardingStore` |
| `service` | `core`, `adapter-iceberg`, Spring Boot, Spring Modulith | the HTTP edge only, as four modules — `party` / `relationship` / `contactpoint` / `onboarding` (ADR-008) |
| `e2e` | `core` (main + test-jar), `adapter-iceberg` (test) | real containers, both latch harnesses, both feature files, the one verdict table |

Dependency direction is `core ← {adapter-iceberg, service} ← e2e`. Nothing points the other way, so the
rules cannot see the engine, the engine cannot see the tests, and `core` never sees Spring. That last one
is enforced rather than trusted: `CoreDependencyBoundaryTest` is an ArchUnit gate that fails the build and
names the offending dependency, and it is itself guarded against the two ways such a rule lies — matching
nothing, and checking nothing.

`service` depends on `adapter-iceberg` and not the reverse, and `e2e` does not depend on `service` at all:
the edge is tested against `core`'s in-memory harness, republished as a test-jar, so there is no second
fake to drift from the first.

## 5. The ports

| Port | Shape | Why this shape |
| --- | --- | --- |
| `SignatoryStore` | `<T> T inTransaction(Function<Tx, T>)` | the same unit-of-work boundary in all three engines; the adapter decides what "transaction" means |
| `Tx` | `countAuthorized`, `setAuthorized`, `touch` | the minimum surface the invariant needs, and the minimum an engine must implement |
| `Lakehouse` | `currentSnapshotId`, `countAuthorizedAt`, `addColumn`, `columnNames`, `authorizedByParty`, `dataFileCount`, `counter`, `renderState` | the analytical half: snapshot pinning, metadata-only evolution, a projected scan, and the raw material for the evidence table |

`SignatoryGuard.revoke` reads the count and then writes one flag — a read predicate the engine cannot
see. `SignatoryGuard.revokeGuarded` adds `touch(partyId)` first, which is the whole of the
application-level mitigation: it gives the transaction something *physical* to collide on.

The onboarding layer states the same idea at a coarser grain — one boundary, one serialization point —
over an aggregate that spans tables:

| Port | Shape | Why this shape |
| --- | --- | --- |
| `OnboardingStore` | `<T> T inTransaction(Function<OnboardingTx, T>)` | one unit of work per submission; the adapter decides what "transaction" means, and here that is the catalog's multi-table commit |
| `OnboardingTx` | reads and writes for `party` / `relationship` / `contactPoint`, plus `touch(partyId)` | the minimum the three invariants need; `touch` is the application-level serialization point |
| `OnboardingLakehouse` | `currentSnapshotId`, `customersByRelationshipType[AsOf]`, `dataFileCount`, `counter`, `renderState` | the analytical half over the onboarding tables, and the raw material for the evidence rows |
| `OnboardingPolicy` | the rules themselves, as pure functions | one place, no engine and no framework: INV-1 count/absence, INV-2 reference, INV-3 uniqueness |

`OnboardingTable` deliberately carries no JDBC, Iceberg or Spring type, which is what keeps `core` at "the
JDK and nothing else" while the adapter and the edge are free to use whatever they need.

## 6. The tables

| Table | Columns | Partition | Retry policy | Role |
| --- | --- | --- | --- | --- |
| `signatory` | `id` (required), `party_id` (required), `authorized` (required) | identity(`id`) | default (`commit.retry.num-retries=4`) | one file per signatory, so two revocations touch different partitions |
| `party_counter` | `party_id` (required), `seq` (required) | identity(`party_id`) | **`commit.retry.num-retries=0`** | the compare-and-set row the mitigation uses |

Partitioning `signatory` by `id` is the mechanism that *permits* the write skew: alice's revocation
replaces the `id=alice` partition and bob's replaces `id=bob`, so neither commit names data the other
one touched. Partitioning the counter by `party_id` is the mechanism that *prevents* it, once the
commit also stops retrying (ADR-003).

The onboarding aggregate is four tables in the same catalog, and that is precisely what makes its unit of
work cross-table:

| Table | Columns | Role |
| --- | --- | --- |
| `party` | `party_id` (PK), `party_type`, `legal_name`, `tax_id` (unique), `kyc_level` | the entity a submission creates |
| `party_relationship` | `relationship_id` (PK), `party_id`, `type`, `status`, `effective_from` | the cross-table reference INV-2 protects |
| `contact_point` | `contact_point_id` (PK), `party_id`, `channel`, `value` (unique with `channel`), `primary` | the contested column: INV-1 is a count over `primary`, so it is the direct successor to `signatory.authorized` |
| `party_counter` | `party_id`, `seq` | the same compare-and-set row as above, reused as the serialization point |

`signatory.authorized` and `contact_point.primary` are the same *shape* of predicate — read a count, write
a row — on entities with different scopes. One is confined to a partition, which is why ordering two
commits can rescue it. The other is not, which is why ordering cannot.

## 7. The commit protocol

```text
inTransaction(action)
   ├─ load `signatory`, pin baseSnapshot
   ├─ action(tx)                     reads use baseSnapshot; writes buffer in memory
   └─ commit()
        ├─ 1. counter  (if touched): OverwriteFiles.deleteFile(<the file read>) + addFile(<new>)
        │      -> refused with CommitFailedException if the base moved (retries are off)
        ├─ 2. signatory (if written): OverwriteFiles.overwriteByRowFilter(<its own id>) + addFile
        │      -> lands regardless; disjoint from the other transaction
        └─ nothing is visible until each commit succeeds; a snapshot is never half-applied
```

Two commits, deliberately ordered. The compare-and-set goes first because if it loses, nothing else is
published and the guarded rule stays intact. That ordering is an application responsibility and not an
engine guarantee — the honest limitation recorded in ADR-006.

That protocol works *because* the finding's invariant is confined to one partition-scoped table. The
onboarding invariants are not: INV-2 spans two tables, so no ordering of per-table commits can be made
safe — a writer that dies between them leaves a relationship pointing at a party that does not exist, and
no retry policy repairs it. The solution is therefore not more ordering but a **different unit of work**:
one `TableCommit` per touched table, handed to the catalog in a *single* call.

```text
inTransaction(action)
   ├─ load `party`, `party_relationship`, `contact_point`, `party_counter`; pin a baseSnapshot per table
   ├─ action(tx)                     reads use their baseSnapshot; writes buffer in memory
   └─ commit()
        └─ catalog.commitTransaction(List<TableCommit>, one per touched table)
             ├─ each commit carries its REQUIREMENTS — an AssertRefSnapshotID on the snapshot it read
             ├─ all or none: the catalog publishes the set as one atomic unit
             └─ a lost race -> ConcurrentCommitException (the edge's 409), nothing published
```

Three consequences are design decisions rather than details:

- **The adapter refuses to start** against a catalog that cannot do this, instead of falling back to
  ordered commits. A silent fallback would reintroduce exactly the half-onboarded party the IT asserts
  cannot exist. `OrderedCommitWriter` is kept in test scope as the *falsifying* comparator: it demonstrates
  that the two-commit ordering being replaced really does expose one, so the crash test proves something.
- **`touch(partyId)` adds nothing on this engine** — the commit is the atomicity. Measured rather than
  assumed: O4 is green with the guard and without it (§11). The guard still earns its place on the
  in-memory reference store, where it is the only mechanism that bites.
- **A `201` is not a durability guarantee.** On the server, a requirement check and a metadata write are
  two steps, so two overlapping requests can both pass — measured at 8/10 with one shared store and 7/10
  with two entirely independent clients, which makes it the catalog's behaviour rather than a client
  artifact. The suite therefore *names the commit order* for the rows it asserts, and **records** the
  overlapping shape instead of asserting it (F-4, ADR-009).

## 8. How the three engines now compare

| | MongoDB `48` | CockroachDB `48` | Iceberg on object storage `49` |
| --- | --- | --- | --- |
| Default isolation | snapshot | strict serializable | snapshot (optimistic commits) |
| Concurrent disjoint rows | both commit | one aborts (`SQLSTATE 40001`) | **both commit** |
| Cross-row invariant | broken | protected by the engine | **broken** |
| Mitigation | hot row causes a write conflict | not needed | compare-and-set table (`retries=0`) |
| Mitigation cost | an abort the app must handle | none | a second commit + an abort the app must handle |
| Cross-table unit of work | not expressible | by the engine | **the catalog's multi-table commit** — measured on the 1.10.1 reference catalog, reasoned on Nessie (ADR-009) |
| Time travel / schema evolution | no | no | **yes** |

The column that changes the decision is the last one: a lakehouse earns its place on the read side
(time travel, schema evolution, scans over the same files) while behaving, on the write side, exactly
like the snapshot-isolated operational store in #48.

## 9. Onboarding: the three invariants, and what protects each

Each rule exists once, in `OnboardingPolicy`, and is named with the anomaly class it belongs to. The
mechanism column is not a design intention — it is the row of §11 that measured it, or the sentence that
says which row does not exist yet.

| # | Invariant | Anomaly class | Predicate | Mechanism that protects it |
| --- | --- | --- | --- | --- |
| INV-1 | a party has at most one **primary** contact point | count / absence (write skew) | count over `contact_point.primary` | the commit's requirement on the `contact_point` snapshot it read — measured, O3 |
| INV-2 | a contact point's party exists in the same commit | cross-table reference (half-write) | existence of `party[party_id]` | the same commit publishes both tables or neither — measured, the crash test |
| INV-3 | `tax_id`, and `(channel, value)`, are unique | phantom / duplicate | absence of an equal row | the commit's requirement on the scanned table's snapshot — see the note below |

INV-1 is the direct successor to the finding: the same shape of predicate — read a count, write a row, the
engine cannot see the predicate — on an entity that spans tables rather than one partition. That is exactly
why the mitigation had to move from "order two commits" to "make them one" (§7).

INV-2 is the one that *cannot* be had any other way. It is also the invariant the signatory PoC never had
to face, because it had a single table; this is the concrete reason the reframe changed the architecture
and not just the table names.

INV-3 carries the one honest gap in this list: it is measured on the in-memory reference harness by unit
tests, and on the lakehouse it rests on the same requirement mechanism as INV-1 rather than on a row of its
own. No O-scenario stages two concurrent inserts of the same `tax_id` yet, so "protected" there is a
reasoned statement, and [ASSESSMENT.md](ASSESSMENT.md) grades it as one.

`touch(partyId)` is retained as the application-level serialization point, and §11 records the measurement
that it is **redundant on the lakehouse** and **load-bearing on the in-memory reference store** — two
engines, one port, opposite mechanisms, which is the comparison the harness exists to make.

## 10. Onboarding: the REST edge, and where a `201` stops

`ltap-service` exposes `POST /onboarding-submissions` behind an `Idempotency-Key` (ADR-008, ADR-009). The
edge is deliberately thin — map the request, claim the key, call the use case, map the result — because
everything interesting lives either in the rule (§9) or in the commit (§7).

| Behaviour | Mechanism | Why it went this way |
| --- | --- | --- |
| a retry is replayed | the recorded first response, byte for byte, with `X-Idempotency-Status: replay` | a client branching on the status must not be able to conclude a second party exists, so the **original** status — `201` included — is returned |
| a key reused with a *different* payload is refused | a fingerprint of the **mapped** request | a retry differing only in case or whitespace is the same submission; one differing in a value is not |
| a refused commit frees the key | `ConcurrentCommitException` is **not** recorded | the point of that refusal is that the client's retry should reach the use case, not be answered from a record as a failure |
| a malformed submission consumes no key | the request is mapped *before* the key is claimed | a bad payload must not burn an idempotency key |
| every `409` is documented | `OnboardingRefusal` and `OnboardingApiExceptionHandler`, cross-checked by `FailureMappingTest` over **every** `OnboardingResult` constant | an unmapped rejection fails the build instead of surfacing as an unexplained `500` |

Two limits are recorded rather than smoothed over:

- **The idempotency record is in-process.** A retry that crosses a restart is refused by INV-3 (a duplicate
  `tax_id`) instead of replayed. The durable version is a fifth table staged into the *same* multi-table
  commit: the mechanism is already here, only the table is missing (T003-007, ADR-009).
- **A `201` is not a durability guarantee.** It means the use case ran and the commit was accepted, which
  F-4 shows is not always the same as the write being in the table. The edge cannot close a gap the catalog
  opened, and implying otherwise would be the more dangerous choice.

## 11. The evidence model

Each scenario logs, through Cucumber's `Scenario.log`, the request, the per-transaction trace
(`READ`/`WRITE`/`COMMIT`/`ABORT`), the committed rows before and after, and the verdict. An `@After`
hook folds the observed outcome into one row of `target/evidence/verdict.md`, and an `@AfterAll` hook
prints and writes the consolidated table — **one table per run, spanning both feature files**
(`EvidenceReport` lives in `io.forest.lakehouse.e2e` so both suites write into it, rather than each
keeping a table that could only be compared by reading prose).

A verdict is computed from the end state, never from the expectation. For the finding:

```text
finalAuthorized < MIN_AUTHORIZED              -> "WRITE SKEW - invariant broken"
failure is a CommitFailedException            -> "PREVENTED - the catalog refused the losing commit"
otherwise                                     -> "PREVENTED - n authorized left"
```

The onboarding rows apply the same principle to `primary`:

```text
finalPrimaries != 1                           -> "WRITE SKEW - INV-1 broken (n primary contact points left)"
a commit refused, the invariants still hold   -> "PREVENTED - <which mechanism refused it>"
guarded and unguarded both hold the invariant -> "PREVENTED", recorded as the measurement it is
```

Two classes of row are deliberately different in kind, and this distinction is the whole reason
[ASSESSMENT.md](ASSESSMENT.md) exists:

- **Asserted** — the deterministic rows (L1–L6, O1–O5). One interleaving, forced by a `CountDownLatch`, so
  a regression changes the verdict table rather than a comment.
- **Recorded** — O6, the overlapping-commit shape (F-4). It pins *only* the invariant, so it stays green
  whether the catalog refuses the loser or accepts both. It exists to keep a real behaviour on the record
  without asserting a promise the server does not make.

## 12. How to run

```bash
mvn -pl core test              # the rules and the in-memory harness: 14 tests, no Docker
mvn -pl service -am test       # the HTTP edge against that same harness: 22 tests, no Docker
mvn -pl e2e -am -Pe2e test     # boots MinIO + the REST catalog, runs everything, tears down
mvn test                       # plain build: no Docker, no e2e suite
```

The two reference onboarding rows need no container at all, so they reproduce anywhere:

```bash
mvn -pl e2e -am -Pe2e test -Dtest=LtapE2EIT -Dsurefire.failIfNoSpecifiedTests=false \
    -Dcucumber.filter.tags="@engine-reference"
```

See the README for `-Dltap.compose.dir` / `-Dltap.compose.manage` and for the two catalog settings
the suite must override.
