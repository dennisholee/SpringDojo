# Write-Skew Isolation PoC — Evidence Report

**Date:** 2026-09-26 · **Git:** uncommitted working tree — this module is untracked; enclosing repository
at `ad406f6`
**Environment:** macOS 26.3 (Apple Silicon, `arm64`) · Docker 27.4.0 · Maven 3.9.14 · JDK 23.0.2
running bytecode compiled with `--release 21` (the project's `<java.version>`)
**Engines:** MongoDB 7.0 single-node replica set `rs0` (`mongo:7.0`) · CockroachDB v23.2 single node
(`cockroachdb/cockroach:latest-v23.2`)

---

## 1. TL;DR — the thesis, with measured values

> **The isolation level, not the topology, decides whether concurrent resignation can break the
> invariant. Sharding never enters the equation; the anomaly is a property of snapshot isolation.**

| Engine | Isolation level (engine default) | Concurrent resignations | Final `authorized` | Verdict |
| --- | --- | --- | --- | --- |
| **MongoDB** | snapshot | **2 / 2 committed**, no error, no retry | **0** | ❌ **WRITE SKEW — invariant silently broken** |
| MongoDB + hot-row counter | snapshot + app mitigation | 1 / 2 committed (`WriteConflict`, error 112) | **1** | ✅ prevented, at the cost of a hot row |
| **CockroachDB** | strict serializable | 1 / 2 committed, other aborted `SQLSTATE 40001` | **1** | ✅ prevented, no application coordination |
| CockroachDB + retry loop | strict serializable + retry | 1 committed, 1 aborted `40001` then **refused on retry** | **1** | ✅ prevented, transient error absorbed |

**Business rule under test —** a party must always keep **at least one** authorized signatory
(`SignatoryGuard.MIN_AUTHORIZED = 1`). Two signatories (`alice`, `bob`) resign at the same instant.

**The headline finding:** on MongoDB both resignations succeeded and the party was left with **zero**
authorized signatories — the *only* trace of the breach is that `authorized` became `0`. No error was
raised, no transaction was aborted, the client saw two successes. On CockroachDB the identical
application code (same `SignatoryGuard`, same store port, same harness) could not produce that state:
one side was aborted and the invariant held at `1`.

---

## 2. What was run

All runs are end-to-end against **real containers**. No mocks, no in-memory database, no engine-side
test hooks. The application code being exercised (`SignatoryGuard`, both adapters) is production code,
unmodified for this report.

| # | Command | Result |
| --- | --- | --- |
| 1 | `mvn -pl e2e -am -Pe2e test` | `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` — **BUILD SUCCESS** |
| 2 | `mvn -pl e2e -am -Pe2e -Dcucumber.filter.tags=@engine-mongo test` | 2 ran, 2 skipped — **BUILD SUCCESS** |
| 3 | `mvn -pl e2e -am -Pe2e -Dcucumber.filter.tags=@engine-cockroach test` | 2 ran, 2 skipped — **BUILD SUCCESS** |
| 4 | `mvn test` (no `e2e` profile) | **BUILD SUCCESS**, zero tests, **no Docker container started** |

Runs 2 and 3 prove the engine tags are independently selectable: each filtered run's verdict table
contained **only** that engine's rows. Run 4 proves the suite is opt-in — a normal build never needs a
Docker daemon.

Each scenario emits its own evidence trail through the Cucumber report (rows before/after the
concurrency, the per-transaction trace, the response and the verdict), so the proof is captured in
`cucumber.json` / `cucumber.html`, not just in console scrollback.

### How the interleaving is made deterministic

Write skew only appears on **one** interleaving: both transactions read, *then* both write. Left to a
scheduler, the threads may run serially by accident — each would see the other's write and the anomaly
would not appear, making the PoC a coin flip. A `CountDownLatch(2)` barrier between the read and the
write forces read-read-then-write-write on every run, so the outcome is reproducible rather than flaky
(see ADR-005).

---

## 3. Evidence, scenario by scenario

Each scenario below shows the three independent pieces of evidence captured by the harness:
**(a)** the committed rows before the concurrency, **(b)** the per-transaction trace (what each thread
actually did), **(c)** the committed rows afterwards, read outside any transaction.

The trace blocks are quoted **verbatim from run 1** of 2026-09-26. One detail is worth stating up front
because it is the difference between a scripted transcript and a real measurement: the forced
interleaving fixes the *shape* (`read-read`, then `write-write`), but **not which thread wins the
conflict**. In run 1 CockroachDB aborted `alice`; in run 2 it aborted `bob`. The traces therefore name
the losing thread as observed in that run, while the end state — `authorized = 0` on MongoDB,
`authorized = 1` on CockroachDB — did not vary (see §3.6).

### 3.1 MongoDB — snapshot isolation permits write skew ❌

`@engine-mongo` · scenario *"concurrent resignations commit without conflict but break the invariant"*

**(a) Before** — `signatory[alice=true bob=true] party_counter.seq=0`

**(b) Trace**

```
mode=plain interleaving=read-read-then-write-write (latch-forced)
tx-bob   | READ   party=P1 -> 2 authorized
tx-alice | READ   party=P1 -> 2 authorized      <-- both baseline on the same 2
tx-alice | WRITE  signatory=alice authorized=false
tx-bob   | WRITE  signatory=bob   authorized=false   <-- different documents: no conflict
tx-bob   | COMMIT revoked=true
tx-alice | COMMIT revoked=true                  <-- both succeed, no error
harness  | VERIFY party=P1 authorized=0 (minimum required=1)
```

**(c) After** — `signatory[alice=false bob=false] party_counter.seq=0`

**Response:** `2/2 committed` — **no error, no retry, no warning.**
**Verdict:** `WRITE SKEW — invariant broken (0 authorized left, both transactions committed)`

`party_counter.seq` is still `0`: neither transaction touched a shared row, which is exactly *why*
nothing conflicted.

### 3.2 MongoDB + hot-row counter — the application-level fix ✅

`@engine-mongo` · scenario *"a hot-row counter closes the gap (application-level mitigation)"*

**(a) Before** — `signatory[alice=true bob=true] party_counter.seq=0`

**(b) Trace**

```
mode=guarded interleaving=read-read-then-write-write (latch-forced)
tx-alice | READ   party=P1 -> 2 authorized
tx-bob   | READ   party=P1 -> 2 authorized
tx-alice | WRITE  signatory=alice authorized=false
tx-alice | COMMIT revoked=true
tx-bob   | ABORT  MongoCommandException: Command failed with error 112 (WriteConflict)
harness  | VERIFY party=P1 authorized=1 (minimum required=1)
```

**(c) After** — `signatory[alice=false bob=true] party_counter.seq=1`

**Response:** `1/2 committed, abort=MongoCommandException error 112 (WriteConflict)`
**Verdict:** `PREVENTED — app-level hot-row counter; one transaction hit a write conflict (1 left)`

Two details worth reading carefully:

- `party_counter.seq` moved **0 → 1**, not `2`. The counter advanced exactly once because the losing
  transaction's increment was rolled back with it. This is the fingerprint of the mechanism: the
  conflict is real, and it is the counter row that caused it.
- The cost is explicit — a **hot row**. Every resignation for a party now contends on one document,
  serialising the very operations that snapshot isolation was happy to run in parallel.

`Tx.touch` is not traced as its own line — the counter write is proven instead by the committed row it
leaves behind: `party_counter.seq` moves `0 → 1`, and only in this scenario. That is a stronger form of
evidence than a log line, because it is read back from the database outside the transaction.

### 3.3 CockroachDB — strict serializable prevents write skew ✅

`@engine-cockroach` · scenario *"concurrent resignations are serialised and the invariant holds"*

**(a) Before** — `signatory[alice=true bob=true] party_counter.seq=0`

**(b) Trace**

```
mode=plain interleaving=read-read-then-write-write (latch-forced)
tx-alice | READ   party=P1 -> 2 authorized
tx-bob   | READ   party=P1 -> 2 authorized
tx-alice | WRITE  signatory=alice authorized=false
tx-bob   | WRITE  signatory=bob   authorized=false
tx-bob   | COMMIT revoked=true
tx-alice | ABORT  SQLSTATE=40001 (PSQLException: restart transaction:
                  TransactionRetryWithProtoRefreshError: TransactionRetryError:
                  retry txn (RETRY_SERIALIZABLE - failed preemptive refresh))
harness  | VERIFY party=P1 authorized=1 (minimum required=1)
```

**(c) After** — `signatory[alice=true bob=false] party_counter.seq=0`

**Response:** `1/2 committed, abort=SQLSTATE=40001`
**Verdict:** `PREVENTED — strict serializable; one transaction aborted with SQLSTATE 40001 (1 left)`

Note what is *identical* and what is *different* versus §3.1. The writes are still to **different
rows**, `party_counter` is still untouched, and the application code paths are the same. Only the
engine's isolation level changed — and that alone turned a silent invariant breach into a detected,
reported conflict. CockroachDB tracked the read-write dependency the application never expressed
(explicitly: `RETRY_SERIALIZABLE` in the driver's own message) and refused to let both commit.

### 3.4 CockroachDB + retry loop — turning a transient abort into a business answer ✅

`@engine-cockroach` · scenario *"a retry loop preserves the invariant and still succeeds"*

**(a) Before** — `signatory[alice=true bob=true] party_counter.seq=0`

**(b) Trace**

```
mode=retry-on-serialization interleaving=read-read-then-write-write (latch-forced)
tx-alice | READ   party=P1 -> 2 authorized
tx-bob   | READ   party=P1 -> 2 authorized
tx-alice | WRITE  signatory=alice authorized=false
tx-bob   | WRITE  signatory=bob   authorized=false
tx-alice | COMMIT revoked=true
tx-bob   | ABORT  SQLSTATE=40001 (...)
tx-bob   | RETRY  after SQLSTATE 40001 -> revoked=false   <-- re-read counts 1, guard refuses
harness  | VERIFY party=P1 authorized=1 (minimum required=1)
```

**(c) After** — `signatory[alice=false bob=true] party_counter.seq=0`

**Response:** `1/2 committed, abort=SQLSTATE=40001`
**Verdict:** `PREVENTED — strict serializable; one transaction aborted with SQLSTATE 40001 (1 left)`

The retry is **not** a second attempt to force the resignation through. On retry the transaction reads
`authorized = 1`, the guard refuses, and it commits as a **definitive refusal** (`revoked=false`).
That is the operationally important property: the caller gets a correct, deterministic answer
("refused, you would have removed the last signatory") instead of a raw serialization error to
interpret.

### 3.5 Consolidated verdict table (as printed by the suite)

```
| Engine | Isolation | Scenario | Result | Final `authorized` | Verdict |
| --- | --- | --- | --- | --- | --- |
| CockroachDB | strict serializable | concurrent resignations are serialised and the invariant holds | 1/2 committed, abort=SQLSTATE=40001 | 1 | PREVENTED - strict serializable; one transaction aborted with SQLSTATE 40001 (1 left) |
| CockroachDB | strict serializable | a retry loop preserves the invariant and still succeeds | 1/2 committed, abort=SQLSTATE=40001 | 1 | PREVENTED - strict serializable; one transaction aborted with SQLSTATE 40001 (1 left) |
| MongoDB (replica set) | snapshot | concurrent resignations commit without conflict but break the invariant | 2/2 committed | 0 | WRITE SKEW - invariant broken (0 authorized left, both transactions committed) |
| MongoDB (replica set) | snapshot | a hot-row counter closes the gap (application-level mitigation) | 1/2 committed, abort=WriteConflict (error 112) | 1 | PREVENTED - app-level hot-row counter; one transaction hit a write conflict (1 left) |

```

The table is derived from the **observed end state**, not from the expected one, so a future
regression shows up as a changed verdict rather than a passing assertion.

### 3.6 Run-to-run stability

The full suite was executed **three times** on 2026-09-26 (run 1, run 2, and a final confirmation run —
same command, same host each time) and `target/evidence/verdict.md` was **byte-identical across all
three** (`diff` reported no differences). What is stable and what is not:

| Observation | Run 1 | Run 2 | Stable? |
| --- | --- | --- | --- |
| MongoDB plain — end state | both commit, `authorized = 0` | both commit, `authorized = 0` | ✅ |
| MongoDB guarded — end state | one abort `WriteConflict` 112, `authorized = 1`, `seq = 1` | same | ✅ |
| CockroachDB plain — end state | one abort `40001`, `authorized = 1` | one abort `40001`, `authorized = 1` | ✅ |
| CockroachDB retry — end state | one refused, `authorized = 1` | one refused, `authorized = 1` | ✅ |
| CockroachDB — *which* thread aborts | `alice` | `bob` | ❌ varies |
| MongoDB guarded — *which* thread aborts | `bob` | `alice` | ❌ varies |

Every invariant-relevant observable — the surviving `authorized` count, the commit count and the error
code — is deterministic; only the identity of the losing thread varies, and no assertion depends on it.
That is precisely why the verdict table is built from **counts and end state** rather than from
"thread A won": a PoC that asserted on the winner would be flaky, and a flaky PoC proves nothing.


---

## 4. Why it happens — the causal chain

The invariant is a **count/absence constraint** ("at least one authorized signatory"), so **no unique
index, foreign key or constraint can express it**. The database has nothing to enforce; the rule lives
entirely in application code (`SignatoryGuard.revoke`).

The two transactions are:

| | reads | writes |
| --- | --- | --- |
| T1 (alice) | `countAuthorized(P1)` → 2 | `signatory/alice.authorized = false` |
| T2 (bob) | `countAuthorized(P1)` → 2 | `signatory/bob.authorized = false` |

Four properties combine, and all four are required:

1. **The reads overlap.** Both transactions read the snapshot as it was *before* either write, so both
   see `2`. This is the only interleaving that shows the anomaly, which is why the latch forces it.
2. **The writes are disjoint.** T1 writes `alice`'s document, T2 writes `bob`'s. There is no
   write-write conflict on any row or document — so a *snapshot-isolated* engine has nothing to detect.
3. **Each transaction's decision depends on the other's write.** T2's write invalidates T1's read (and
   vice versa): had T1 seen T2's write, it would have read `1` and refused. This is a **read-write
   dependency cycle**, and it is precisely what snapshot isolation is blind to.
4. **Both commit.** Terminal state `0 authorized`. **No serial order of T1 and T2 can produce this
   state** — run them one after the other and the second always reads `1` and refuses. That is the
   textbook definition of write skew, and it is why a "successful" pair of transactions can still have
   broken the invariant.

Where the engines differ is only **how much of step 3 they can see**:

- **MongoDB (snapshot):** multi-document transactions are snapshot isolated. Engine defaults are used
  as-is; the isolation level is never overridden in the PoC. Disjoint writes ⇒ no conflict ⇒ both
  commit ⇒ **invariant silently broken**.
- **CockroachDB (strict serializable):** SERIALIZABLE tracks read-write dependencies between
  transactions, so the cycle in step 3 is detectable *even though the writes are disjoint*. One
  transaction is aborted with `SQLSTATE 40001` (`RETRY_SERIALIZABLE`) ⇒ **invariant held**.

Note the asymmetry that matters commercially: the failure mode of the snapshot engine is **silent data
corruption** (a party with no authorized signatory, discovered later by audit or by a downstream
process), whereas the failure mode of the serializable engine is a **loud, retryable error** raised at
the point of write.

---

## 5. Mitigation options for a snapshot-isolated store

If MongoDB is the mandated store, correctness must be **engineered into the application**:

| Option | Mechanism | Evidence here | Trade-off |
| --- | --- | --- | --- |
| **Hot-row counter** (used here) | Every mutation first writes a shared `party_counter` row, manufacturing a write-write conflict | §3.2 — `seq 0→1`, one txn aborted `WriteConflict` 112, final `1` | Serialises all mutations for that party; the guard row is a contention hotspot and must be keyed deterministically |
| Application lock / lease | Distributed lock around read-modify-write | not implemented | Extra infrastructure; lock expiry and failure modes are their own correctness problem |
| Predicate / parent-row write | Write the parent `party` row instead of a bare counter | not implemented | Same hot-row cost, arguably clearer domain modelling |
| Unique index / constraint | — | **not possible** | A count/absence rule has no index expression; this is the crux of the problem |

The counter approach is correct and cheap to add, but it is a **design concession**: it reintroduces
the serialisation point the engine chose not to provide, and it must be applied to *every* code path
that touches the invariant. One forgotten path reopens the hole — with no runtime signal.

---

## 6. Architecture conclusion

> **Topology is a red herring. The isolation level is the deciding factor.**

- **Sharding does not change this result.** A MongoDB *sharded* cluster still runs snapshot-isolated
  multi-document transactions, so it still permits write skew (ADR-006). The demo runs single-node
  precisely because the phenomenon is an isolation property, not a distribution property: adding nodes
  would add operational noise without changing a single step of the causal chain in §4.
- **The observable is the engine contract, not the vendor.** The same `SignatoryGuard`, the same
  `SignatoryStore` port and the same latch harness ran against both engines. Only the adapter (and
  therefore the isolation level) changed, and the outcome flipped. That is why the invariant lives in
  `core` (engine-agnostic) while the isolation guarantee is delivered by the adapter.
- **Pick the engine by the failure mode you can accept.** Snapshot isolation buys throughput and avoids
  spurious aborts, and charges for it in *silent invariant breaches the application must prevent*.
  Strict serializable buys correctness with no application coordination, and charges in *retryable
  `40001` aborts the application must handle*.
- **Both engines require application work — just different work.** With MongoDB: **prevention**
  (hot-row / lock design). With CockroachDB: **absorption** (a retry loop). Neither is free, and
  "the database will protect my invariant" is false in the snapshot case.

---

## 7. Scope, limits and how to falsify this

**What this evidence establishes.** For the given invariant, this interleaving, these two engines and
their default isolation levels, the outcome is as stated above — and it reproduced on every run.

**Honest limits — read before quoting the result:**

- **Existence proof, not a frequency estimate.** The interleaving is *forced* by a latch. This proves
  the anomaly is reachable in the real engine; it says nothing about how often production traffic hits
  it. Under load the vulnerable window is "from the read until the first writer commits", which in real
  systems is wider than intuition suggests.
- **Single-node, default isolation.** One invariant (`MIN_AUTHORIZED = 1`), one party, two signatories,
  one interleaving shape, no read-concern / write-concern tuning and no explicit isolation overrides.
  The results apply to the defaults actually exercised.
- **The guarded scenario validates one mitigation, not all of them.** The alternatives in §5 are
  reasoned from the mechanism but are not implemented or measured here.

**How to falsify the thesis.** Any of the following would undermine §6 and should be attempted before
generalising:

1. Raise MongoDB's isolation to serializable for these transactions and show the skew still occurs.
2. Lower CockroachDB to snapshot and show the skew appears — i.e. the isolation level, not the vendor,
   drove the result.
3. Deliberately omit the counter write on one guarded code path and show the invariant still breaks —
   demonstrating the mitigation is a discipline, not a guarantee.

---

## 8. Appendix — raw artifacts and reproduction

**Reproduce** (from the repository root; the `e2e` profile needs Docker):

```bash
mvn -pl e2e -am -Pe2e test                                   # both engines, all 4 scenarios
mvn -pl e2e -am -Pe2e -Dcucumber.filter.tags=@engine-mongo test
mvn -pl e2e -am -Pe2e -Dcucumber.filter.tags=@engine-cockroach test
```

**Artifacts produced by each run** (regenerated on every `e2e` run; `target/` is git-ignored):

| Artifact | Contents |
| --- | --- |
| `e2e/target/evidence/verdict.md` | the consolidated verdict table of §3.5 |
| `e2e/target/cucumber-reports/cucumber.html` | human-readable report: before/after rows, full trace, response and verdict per scenario |
| `e2e/target/cucumber-reports/cucumber.json` | machine-readable equivalent, for tooling and dashboards |

**Where each piece of evidence originates** (all test-scoped — no production logic was changed to
produce this report):

| Evidence | Source |
| --- | --- |
| before/after committed rows | `MongoFixture.snapshot()` / `CockroachFixture.snapshot()` |
| per-transaction trace (READ / WRITE / COMMIT / ABORT / RETRY, per thread) | `ConcurrentRevoke.attempt` → `Outcome.trace` |
| response (commits + SQLSTATE or driver error) | `SignatorySteps.describeResponse` + `ConcurrentRevoke.describe` |
| verdict row and consolidated table | `SignatorySteps.verdict` / `EvidenceReport` |

**Engines under test:** MongoDB 7.0 single-node replica set `rs0` (`docker/docker-compose.mongo.yml`);
CockroachDB v23.2 single node (`docker/docker-compose.cockroach.yml`, schema
`docker/init-cockroach.sql`). Both are torn down (`docker compose down -v`) at the end of each run —
verified after the runs above: no `write-skew` containers or volumes remained.

**Related documents:** `README.md` (§10 expected results, §12 build and run) ·
`docs/adr/ADR-001`…`ADR-006` · `docs/DESIGN.md`.
