# ADR-005: Latch-Based Deterministic Concurrency Harness

## Problem statement
Write skew appears only on one interleaving (read-read, then write-write). How do we make the
demonstration reproducible instead of a flaky race?

## Background
Without forcing the interleaving, two threads may run serially by accident, in which case each
transaction sees the other's write and the Mongo scenario would *not* exhibit write skew — producing
a misleading pass/fail depending on thread scheduling. `ConcurrentRevoke` therefore barriers both
transactions between their read and their write.

## Constraints
- Both transactions must complete their reads before either writes.
- The harness must be engine-agnostic (driven through `SignatoryStore`).
- It must capture committed/error/final-count per thread for the verdict table.

## Assumptions
- A `CountDownLatch(2)` barrier between read and write is sufficient and correct.
- The retry-on-`40001` path is exercised only for the CockroachDB scenario.

## Options

| | Latch (barrier) harness | Random concurrent stress | Simulated interleaving (mocks) | Serialised test |
| --- | --- | --- | --- | --- |
| Summary | Barrier forces read-read then write-write | Many iterations; observe statistically | Manually step `Tx` calls in test code | Run transactions one after another |
| Determinism | High | Low (probabilistic) | High, but not real | Deterministic, but cannot show skew |
| Exercises the real engine | Yes | Yes | No | Yes |
| Score (out of 5) | 5 | 2 | 2 | 1 |
| Remarks | Chosen — mandatory. | Rejected: still a race. | Rejected: mocks would not prove engine behaviour. | Rejected: cannot produce write skew. |

## Consequences
- The Mongo case deterministically commits both transactions and breaks the invariant.
- The CockroachDB case deterministically aborts one with `SQLSTATE 40001`.
- The harness is test-time only; no latch logic leaks into production code paths.

## Addendum: harness additions and verified outcome

The harness was implemented and executed against the real engines (MongoDB 7.0 replica set `rs0`,
CockroachDB v23.2 single node) on 2026-09-26. Three additions beyond the literal README were needed
to make the proof observable end to end; none of them change the interleaving the barrier forces.

| Addition | Why |
| --- | --- |
| `Tx.touch(partyId)` | The guarded-party scenario must read *and write* the shared counter row inside the transaction; that row is what makes Mongo's snapshot isolation conflict where the per-signatory documents do not. |
| `SignatoryGuard.revokeGuarded(tx, partyId, signatoryId)` | One entry point performing the `touch` and the guarded `revoke` in a single transaction, so both adapters implement the mitigation identically and the rule stays in the engine-agnostic core. |
| `Outcome.aliceRevoked` / `Outcome.bobRevoked` | The verdict table must distinguish "one committed" from "one resignation actually took effect", which `commits()` alone cannot express. |

`party_counter` exists in both schemas (a Mongo collection and a CockroachDB table) so the adapters
stay symmetric; on CockroachDB it is simply a second row the serializable check may cycle through.

### Verified outcome

Each scenario is tagged `@engine-mongo` or `@engine-cockroach`, so either engine can be run alone.

| Engine | Scenario | Observed |
| --- | --- | --- |
| MongoDB | plain concurrent resign | both commit, `finalAuthorized = 0` — write skew |
| MongoDB | guarded party | exactly one commits, `finalAuthorized = 1` — skew prevented |
| CockroachDB | plain concurrent resign | one commits, the other fails with `SQLSTATE 40001`, `finalAuthorized = 1` |
| CockroachDB | retry on `40001` | one resigns, the other is refused on retry, `finalAuthorized = 1` |

### Build notes

- Surefire runs with the `e2e` module as its working directory, so `Compose.file(name)` resolves the
  Compose files from `../docker`; override the location with `-Dwrite-skew.compose.dir`.
- `org.junit:junit-bom` is imported in the root POM. Cucumber 7.34.8 declares
  `junit-platform-engine`/`commons` 1.14.2 while `junit-platform-suite` pins 1.13.4, and mixing the
  two releases fails every engine with
  `NoSuchMethodError: ExecutionRequest.create(TestDescriptor, EngineExecutionListener, ConfigurationParameters, OutputDirectoryProvider, NamespacedHierarchicalStore)`.
  The import aligns every platform artifact on 5.13.4 / 1.13.4.
- The run commands pass `-am` so `core` and the adapters come from the reactor; without it,
  `-pl e2e` requires a prior `mvn install`.
- Cucumber's step-definition arity check rejects `io.cucumber.java.Scenario` as a *step* parameter
  (`Step [...] is defined with 1 parameters ... However, the gherkin step has 0 arguments`). `Scenario`
  is therefore injected only into the `@Before`/`@After` hooks, and the steps read it from a field the
  hook assigns.
- Feature discovery uses `@SelectPackages("features")` on the suite entry point. The obvious
  `@SelectClasspathResource("features")` also works, but makes Cucumber log *"The classpath resource
  selector 'features' should not be used to select features in a package"* on every run. The package
  selector discovers the same two `.feature` files with no warning, and both the scenario count (4) and
  the tag filtering (`@engine-mongo` / `@engine-cockroach`, 2 ran / 2 skipped each) are unchanged.

### Addendum: evidence capture

The scenarios must not merely pass — they must produce the numbers a reader can check. Three
test-scoped additions carry the proof into the Cucumber report (`cucumber.html` / `cucumber.json`), so
the evidence travels with the build instead of living in console scrollback:

| Addition | Why |
| --- | --- |
| `Outcome.trace` + per-attempt trace lines | Records what each thread actually did (`READ`/`WRITE`/`COMMIT`/`ABORT`/`RETRY`) with the interleaving mode, so the forced read-read-then-write-write order is visible in the report. |
| `MongoFixture.snapshot()` / `CockroachFixture.snapshot()` | Reads the committed rows *outside* any transaction, before and after the concurrency; this is the observable that distinguishes "one committed" from "the invariant held". |
| `EvidenceReport` | Collects one verdict row per scenario and writes the comparison table to `target/evidence/verdict.md`. |

The rows are derived from the observed end state, so a regression changes the verdict instead of
failing silently. The executed results are written up in
[`../reports/write-skew-evidence-2026-09-26.md`](../reports/write-skew-evidence-2026-09-26.md).

