# ADR-009: Cross-Table Atomicity and Idempotency for the Onboarding Submission

## Problem statement
INV-2 requires a party, its relationship and its contact points to become visible together. Iceberg
commits are **per table** (ADR-006), and the PoC's answer — publish two commits and order the
compare-and-set first — is an application convention, not a guarantee: a crash between the commits
leaves a party with no contact point. How should onboarding obtain atomicity across tables, and how
should a retried submission be prevented from creating a second party (INV-3)?

## Background

The primitive this ADR was looking for is an atomic **multi-table commit**. The client side is
`org.apache.iceberg.catalog.TableCommit` — a table's identifier, its `UpdateRequirement`s and
its `MetadataUpdate`s — handed to `RESTCatalog#commitTransaction(List<TableCommit>)`, which the REST
protocol carries as `POST /v1/{prefix}/transactions/commit`. `TableCommit.create(identifier, base,
updated)` derives the requirements itself, and the decisive one is `AssertRefSnapshotID` on the branch.

Four corrections to what this ADR assumed when it was first written, each checked against the 1.10.1
artifacts and against a running catalog:

- The mechanism is **the catalog's REST transaction endpoint**, not a Nessie-specific API. Nessie's
  Iceberg REST catalog serves that endpoint, which is why Nessie stays the production recommendation;
  but the reference catalog this PoC already runs (`apache/iceberg-rest-fixture`) serves it too, and
  advertises it in `GET /v1/config`. The mechanism is therefore demonstrated on the stack that is
  already here rather than on one added for the occasion.
- `NessieCatalog`, the native Nessie integration, does **not** expose it: it extends
  `BaseMetastoreViewCatalog`, is not a `SessionCatalog`, and `SessionCatalog` declares no
  `commitTransaction` at all. Only the REST client has it.
- The earlier claim that the reference catalog "exposes one-table commits only" was **wrong**. It was
  inferred from `javap` on `RESTCatalogAdapter`, whose handlers are registered as lambdas in a static
  map and are therefore invisible to reflection-based inspection. Recorded here because it cost a round
  of investigation: probe the endpoint, do not infer it from the class file.
- The claim that Iceberg **1.8** "added" this primitive was **wrong**. `javap` on the artifacts already in
  this repo shows `TableCommit.create(...)`, both `RESTCatalog.commitTransaction` overloads and
  `ResourcePaths.V1_TRANSACTIONS_COMMIT` present in the **1.7.1** client, so no release boundary is claimed
  for the client types. What is version-specific is the *server* that serves the endpoint — which is why the
  PoC runs a 1.10.1 client against the 1.10.1 reference server.

## Constraints
- The guarantee MUST be a transaction guarantee, not an ordering convention, or INV-2 is not met.
- `core` MUST NOT learn which catalog is used (ADR-002/ADR-008); the port is
  `OnboardingStore.inTransaction`.
- A failing transaction MUST surface as `ConcurrentCommitException`, the type the core already names.
- Idempotency MUST be enforced at the edge, because the store cannot see a retried HTTP request.

## Assumptions
- One Nessie commit over the onboarding tables is an acceptable unit of atomicity for a submission; a
  submission does not need to atomically touch anything outside those tables.
- Branch-per-request is unnecessary; a shared branch with optimistic conflict detection is sufficient at
  this concurrency.

## Options

| | `iceberg-rest-fixture` + ordered commits (status quo) | **Nessie multi-table commit** | Apache Polaris | One denormalized table | Outbox + repair job |
| --- | --- | --- | --- | --- | --- |
| Cross-table atomicity | No — ordering only | **Yes, one commit** | Per-table commits; no cross-table transaction | Yes, trivially | Eventually consistent |
| Crash between commits | Half-onboarded party | Impossible | Possible | Impossible | Possible, then repaired |
| Catalog/RBAC maturity | Fixture, local only | Mature, git semantics, REST + native API | Graduated TLP, strong RBAC and credential vending | N/A | N/A |
| Impact on the domain | None | None — swaps the catalog behind the adapter | None | Rewrites the model around one wide table | Adds a second system and a job |
| Cost | Low (already built) | Low | Low | Medium (loses the relational shape) | High (a new failure mode to operate) |
| Score (out of 5) | 1 | 5 | 3 | 3 | 2 |
| Remarks | Rejected: it is the limitation this ADR exists to remove. | **Chosen.** | Kept for the RBAC/credential story (security), not for atomicity. | Rejected: one table per aggregate is not tolerable for `party`/`relationship`/`contactpoint` reads. | Rejected: acceptable only if atomicity is genuinely unavailable, which it is not. |

## Architecture view

```text
inTransaction(action)
   ├─ load the four tables once; capture each one's metadata == the snapshot this tx reads
   ├─ action(tx): buffer party / relationship / contact-point rows, plus touch(partyId)
   └─ commit()
        ├─ for each touched table: stage the snapshot (append, or overwrite naming the files it read)
        ├─ TableMetadata.buildFrom(base).addSnapshot(staged).setBranchSnapshot(.., "main")
        ├─ TableCommit.create(identifier, base, updated)     -> requirements + updates derived
        └─ RESTCatalog.commitTransaction(List<TableCommit>)  -> POST /v1/{prefix}/transactions/commit
             all four tables applied, or none
             AssertRefSnapshotID fails -> CommitFailedException -> ConcurrentCommitException
```

Nothing is visible until the catalog accepts the whole list, so there is no half-applied submission.
The order the `TableCommit`s appear in carries no meaning: there is one catalog call, not four.

## Verification (2026-09, Iceberg 1.10.1 client against the 1.10.1 reference catalog)

| Check | Observed |
| --- | --- |
| One commit publishes two tables | `commitTransaction([t_a, t_b])` → `t_a=[a1] t_b=[b1]`, each with `requirements=2 updates=2` |
| A lost race refuses the whole transaction | after another writer moved `t_a`: `CommitFailedException: Requirement failed: branch main has changed: expected id … != …`, and `t_b` did **not** gain its row — the transaction rolled back across tables |
| The onboarding use case over four tables | `OnboardingOnIcebergIT`: party + relationship + contacts appear together; an abandoned unit of work leaves `Optional.empty`, `counter=-1`, no contacts |
| The ordered two-commit design it replaces | `OrderedCommitWriter` (test-scope comparator): the party **is** visible with no relationship and no contact point |
| Conflict granularity | per table, at the branch: a concurrent write to *any* of the four tables refuses the whole transaction |
| Two demotions of *different* primary contact points, commits **sequenced** | The loser is refused (`Requirement failed: branch main has changed`) and the party keeps one primary contact point — the row `OnboardingOnIcebergIT` pins, reproduced by Cucumber O3 |
| The same two commits allowed to **overlap** | **Both were accepted** (`2/2 committed`) and one demotion vanished: a transaction whose client was told it had succeeded is not in the table, and its `party_counter` increment went with it. The invariant held anyway — each snapshot is a full replacement built from the same base, so the survivor is one demotion — by accident, not by protection (F-4) |

Idempotency is handled one layer out, in `ltap-service` (T003-007). The REST edge requires
`Idempotency-Key`, stores the first result keyed by it, and replays that result (with
`X-Idempotency-Status: replay`) on a retry. The `onboard` use case itself is idempotent on
`partyId`/`taxId`, so the key is a convenience rather than the only defence: a retry that arrives with a
fresh key is still refused by INV-3.

### The idempotency decisions, as implemented

| | Decision | Why, and what it rejects |
| --- | --- | --- |
| What a replay returns | The original status **and** body, byte for byte, plus `X-Idempotency-Status: replay` | Rejected: `200` on replay. A client branching on the status would see the same key answer `201` and then `200`, and could conclude a second party had been created. The stored body is the *rendered* text rather than a re-rendered object, so a replay cannot drift into a slightly different answer. |
| What a key identifies | One `(operation, key)` pair | Rejected: the key alone. It costs nothing today and stops one write endpoint answering for another the moment a second one exists — the same shape the dojo's `05_IdempotencyKey` stores (`METHOD:PATH` plus key). |
| What the fingerprint covers | The **mapped** request, not the raw body | The mapping has already trimmed the text, normalised the vocabulary's case and read a missing contact-point list as an empty one, so a retry that differs cosmetically is the same submission while one that differs in a value is not. Fingerprinting the bytes gets both of those backwards. |
| Which outcomes are remembered | The deterministic ones: the `201`, and the refusals the domain *returns* (a duplicate tax id stays a duplicate tax id) | Rejected: remembering everything. A `ConcurrentCommitException` is the one refusal that means *try again*; recording it would pin the client to a `409` for a key that never committed anything. It therefore leaves the store as an exception, the key is released, and the retry reaches the use case. |
| When the key is claimed | After the request has been read into the domain's vocabulary | Rejected: claiming first. A malformed body would consume a key it never used, and the client's corrected submission — sent with the same key, which is *what a client does* — would then be refused as a reuse. |
| Where the record lives | In-process, for now | The honest limitation: the record dies with the process, so the guarantee is "a retry is answered within one process lifetime". The fix needs no new mechanism — the multi-table commit above could carry a fifth table, the record, in the *same* transaction as the party, making the key and the party inseparable. A deferral, not a property. |

That last row is also why the edge is not the only defence. With a durable record a retry is always
answered; without one it is INV-3 that refuses the second party. The invariant is the guarantee, the key
is the courtesy.

## Consequences
- INV-2 stops being an application responsibility: the ordering convention, and the crash window it
  left open, both disappear from the adapter. The "counter first" convention disappears with it — the
  counter may be staged anywhere in the unit of work, because the conflict is decided by the commit's
  requirement, not by the order the writes were buffered in.
- **`touch(partyId)` narrows further than expected -- for sequenced commits.** A `TableCommit` asserts the
  branch snapshot of every table it names, so two demotions of *different* contact points both write the
  `contact_point` table and the second commit is refused. INV-1's write skew is therefore closed by the
  catalog here, not by the counter. But "the second commit is refused" is not unconditional, and the e2e
  suite measured the exception: with the two commits in flight at once both were accepted and one demotion
  vanished (F-4). The counter stays because it is the *portable* mitigation — it is what protects INV-1 on
  a catalog without the transactions endpoint, and on the plain single-table write path the signatory PoC
  measured. It does not make an overlapping commit safe either: in that same run one of two guarded
  increments was lost. It is a belt, not the braces, and the real answer to F-4 is a commit protocol that
  cannot accept both.
- Be explicit about what that costs: **this adapter is stricter than snapshot isolation.** Any
  concurrent write to any onboarding table refuses the transaction, so concurrency on a hot table is
  bounded by contention rather than by the invariant. For onboarding — one write-once party per
  submission — that is the right trade; a high-contention mutable table would need revisiting.
- `NessieCatalog` cannot deliver this; the adapter binds to the REST client and refuses to start
  against a catalog that cannot publish several tables in one action, rather than silently falling back
  to ordered commits.
- Polaris is not discarded: it remains the candidate for RBAC and credential vending once the write
  path is settled, and can front Nessie's catalog if both are wanted.
- Left open: `e2e` still boots the reference REST catalog rather than Nessie. Nessie serves the same
  endpoint, so this is a container swap plus a fixture property, but until it is done the
  Nessie-specific half of this decision is reasoned rather than measured.
- **F-4, root-caused (2026-09-27): the reference catalog's multi-table commit is not atomic across
  concurrent requests.** Two hypotheses were open — the server's check-then-act in `CatalogHandlers`
  (validate `request.requirements()` against `ops.current()`, then `ops.commit(base, updated)`, with
  nothing guarding the gap), and a client-side one, that the two units of work share one cached `Table`
  instance. Measured, the client hypothesis is **refuted**: `RESTCatalog.loadTable` returns a distinct
  `BaseTable` per call (there is no table cache), and the lost update reproduces with two **independent
  stores — two catalogs and two HTTP clients — at the same rate as with one**. Both transactions assert
  `main == ` the same base id; when the two requests are in flight at once the requirement check and the
  metadata write are not one atomic step, so both pass and the later write discards the earlier one.
  Rates over 10 attempts each against the 1.10.1 reference catalog on SQLite: **sequenced 0/10** (the
  loser is refused every time, deterministically), **overlapping on one store 8/10**, **overlapping across
  two independent clients 7/10**. In every both-accepted case two demotions were staged and the party was
  left with one primary contact point: exactly one demotion, whose client was told the commit had
  succeeded, is not in the table.
  The e2e scenario still records rather than asserts it, and the finding is not fixable from this repo —
  it is the reference fixture's server-side behaviour. What the REST edge can promise meanwhile is
  unchanged, and is now a measurement rather than a suspicion: a `201` means the use case ran and the
  commit was accepted, which is not always the same as the write still being in the table.
- Left open: the idempotency record is in-process, so a retry that crosses a restart is refused by INV-3
  rather than replayed. The fix is a fifth table staged into the same `TableCommit` batch as the party.
- Left open: **an abandoned unit of work leaves its Parquet files in the bucket.** The rows are staged
  into `AppendFiles`/`OverwriteFiles` with `stageOnly()`, and `writeFile` writes each data file while the
  commit is still staged, so aborting publishes nothing *visible* — which is exactly what INV-2 requires
  — but does not reclaim the bytes. Nothing reads them (no snapshot references them), so this is a
  storage-hygiene issue rather than a correctness one; a production deployment needs a periodic
  `remove_orphan_files`, or a delete-on-abort path. Recorded because "the writer that dies leaves nothing
  behind" is true of the *table*, and must not be read as true of the *bucket*.
