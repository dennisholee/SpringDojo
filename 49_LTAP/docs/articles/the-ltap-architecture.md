# The LTAP Architecture: One Copy of the Data, Two Ways to Read It

*LTAP is this project's name for the lakehouse transactional–analytical pattern: one physical copy of your
rows on object storage — an open table format — kept transactional enough to write to, analytical enough to
reason over, and versioned enough to audit. The goal is to stop paying for a second copy of the data and for
the ETL that keeps the two halves honest.*

> **Masthead — the architecture on one page.** *The two-copy tax on the left; the answer on the right: one open
> table format on object storage, written by one unit of work through one catalog action, read transactionally
> and analytically off the same files — with the two failures the harness actually recorded shown, not hidden.*

**Prompt for Gemini:**

```text
The LTAP architecture at a glance — one copy of the data, two ways to read it

Single-panel technical visual abstract, 16:9, white background, generous margins, flat vector, clean
geometric sans-serif type, no gradients, no 3D, no shadows, no photography, no mascots. Leave the top
tenth of the canvas blank, so an article headline can sit above the image. Read it left to right as
three acts, with a thin vertical divider between acts 1 and 2.

Act 1, left third, muted grey, headed "The two-copy tax": a database cylinder labelled
"Transactional DB" and a warehouse box labelled "Analytical warehouse", joined by a winding arrow
labelled "ETL / CDC / orchestration DAG", with three small red minus tags beside that arrow —
"storage", "movement", "trust".

Act 2, centre and right two thirds, full colour, headed "One copy of the data, two ways to read it":
a vertical stack of five labelled bands, top to bottom — "API client and HTTP edge (Idempotency-Key)",
"core — JDK only: rules, ports, INV-1 / INV-2 / INV-3", "adapter-iceberg — raw Iceberg Java API",
"Iceberg REST catalog — one atomic decision", "object storage — Parquet files + metadata". One thick
blue arrow descends through all five bands, labelled "one unit of work → one catalog action", and a
thin note beside it reads "a commit asserts the snapshot it read". From the bottom band a green arrow
branches right to a box labelled "Analytical read — same Parquet files: snapshot pin, projected scan,
time travel". Beside the core band, three small chips, each pairing an invariant with its anomaly
class: "INV-1 count/absence → write skew", "INV-2 cross-table reference → half-write",
"INV-3 uniqueness → phantom".

Act 3, a full-width amber strip across the bottom, headed "Evidence, not assertion": a miniature
verdict table with three rows — "sequenced → loser refused 10/10, updates lost 0/10";
"overlapping → both accepted, one write vanishes 8/10 (7/10 across two clients)";
"verdict → WRITE SKEW, and LOST UPDATE recorded (root cause F-4)" — with three small badges to its
right: "measured", "reasoned", "recorded".

Two footnote lines in small regular type, bottom left: "snapshot isolation, not serializability" and
"history is an audit trail bounded by retention".
Palette: grey for the problem act, blue for the write path, green for the read path, amber for
evidence, dark slate for text; every colour repeats a text label so colour is never the only signal.
Keep total on-image text under about 60 words by rendering only the strings above: no lorem ipsum, no
invented labels, no watermarks, no extra annotations.
```

**What you'll learn**

- Why running the same data twice costs you in storage, pipelines, and trust.
- How an open table format on object storage delivers atomic commits, time travel, and schema evolution over one copy.
- Where snapshot isolation breaks a count/absence invariant, and the design that closes the gap.
- How to attach an evidence class — **measured**, **reasoned**, or **recorded** — to every claim.

**Prerequisites.** Java 21, Maven, and Docker with the Compose v2 plugin. The reference stack pins Apache
Iceberg 1.10.1 and Spring Boot 3.5.16 with Spring Modulith 1.4.13 (edges only); re-verify the versions at
adoption time.

> **Figure 1 — LTAP in one line.** *Several producers write to one open table format on object storage; two
> readers fan out from it — a transactional read of the current snapshot and an analytical read over the same
> files — and the snapshot list is the audit trail while retention keeps it. There is exactly one storage
> box, not two.*

**Prompt for Gemini:**

```text
Figure 1 — LTAP in one line

Flat vector technical infographic, 16:9, white background, thin strokes, clean sans-serif type, no
gradients, no 3D, no shadows, no photography, no decorative icons.
Headline, top left, one line: "One copy of the data, two ways to read it".
Left third: three stacked producer boxes, each with a thin arrow pointing right —
"Transactional app", "Ops console", "Partner API".
Centre third: one large rounded box labelled "Open table format on object storage", holding three
stacked layers labelled "Parquet data files", "Manifest + metadata", "Snapshot list (audit trail)".
All three producer arrows converge into this box; directly beneath it, small type reads
"retention bounds how far back the list goes".
Right third: two reader boxes fanned out of the same centre box by two arrows, top first —
"Transactional read — current snapshot", then "Analytical read — same Parquet files, projected scan,
time travel".
Far right, clearly separated from the single storage box: a crossed-out database cylinder labelled
"no second copy — no warehouse, no ETL".
Palette: one accent blue for the write path, one accent green for the read path, neutral grey for
storage; every colour repeats a text label so colour is never the only signal.
Render only the strings above: no extra text, no watermarks, no invented annotations.
```

## In this article

1. [Introduction: the two-copy tax](#1-introduction-the-two-copy-tax)
2. [Solution overview: one write path, one read path](#2-solution-overview-one-write-path-one-read-path)
3. [The four concepts that do the work](#3-the-four-concepts-that-do-the-work)
4. [Key designs, in depth](#4-key-designs-in-depth)
5. [Inputs, logic, and outputs](#5-inputs-logic-and-outputs)
6. [Key features, mapped to the repository](#6-key-features-mapped-to-the-repository)
7. [What the architecture measures — and what it does not](#7-what-the-architecture-measures--and-what-it-does-not)
8. [Where this applies](#8-where-this-applies)
9. [Why LTAP, and not the alternatives](#9-why-ltap-and-not-the-alternatives)
10. [Closing notes](#10-closing-notes)

The masthead at the top and every figure below are given as an **infographic prompt** instead of as markup:
each is a caption with a fenced block beneath it, and pasting that block into Gemini — or any image model —
reproduces the picture the prose describes. The article is written to stand on its own, so nothing is lost if
you never generate them; the prompts exist so that a generated image says exactly what the text says, down to
the labels.

---

## 1. Introduction: the two-copy tax

Every enterprise runs the same data twice.

The transactional copy lives in an operational database and answers *"is this write allowed, and did it
happen?"* The analytical copy lives in a warehouse or lakehouse and answers *"what does the business look
like now, and last quarter?"* Between them sits a pipeline: batch extracts, change-data-capture, an
orchestration DAG somebody inherits and nobody volunteers to own.

You pay for that second copy three times. You pay to **store** it. You pay to **move** it, and to keep moving
it as schemas drift. And you pay in **trust**, because two copies are two sources of truth wearing one name:
the transactional copy accepts a write, the analytical copy learns about it minutes or hours later, and a
decision made on the stale copy is a decision you cannot undo.

The interesting question is not how to make the pipeline faster. It is whether the second copy has to exist
at all. Can one physical copy of the rows — an open table format on object storage — be written to
transactionally *and* read analytically, with its own version history kept as a first-class artifact? That is
the claim **LTAP** tests.

Four capabilities carry the claim. Each is a requirement, not a slogan:

1. **Atomic commits on object storage.** A write either becomes a visible snapshot or it does not; there is no half-applied state for a reader to trip over.
2. **A cross-entity unit of work.** Several tables publish together, so an aggregate that spans tables is never half-written.
3. **Analytical reads over the same files.** Time travel, schema evolution, and projected scans need no second engine and no copy.
4. **Evidence, not assertion.** A harness forces one interleaving and reports the observed end state as a table.

The fourth is the least glamorous and the most important: a concurrency claim you cannot reproduce is a
rumour; one that arrives with a verdict table is an engineering input.

This article is a build report, not a vendor pitch. It comes from a repository (`49_LTAP`) that tested the
claim: one framework-free domain core, one Apache Iceberg adapter over object storage, one thin HTTP edge, and
a harness that computes a verdict from observed end state rather than from expectation. Along the way it
produces a genuinely useful negative result — a lakehouse's ACID is *snapshot isolation*, not serializability
— and then shows what you build differently once you know that.

---

## 2. Solution overview: one write path, one read path

The architecture has one write path and one read path, and they meet at the same files.

> **Figure 2 — Architecture.** *The write path runs client → edge → core → adapter → catalog and object
> storage, and never touches a database. The read path leaves from the same object storage, so an analytical
> query reads the same bytes a transactional read sees.*

**Prompt for Gemini:**

```text
Figure 2 — Architecture: one write path, one read path

Flat vector architecture diagram, 16:9 landscape, white background, thin strokes, rounded rectangles,
clean sans-serif type, no 3D, no shadows, no photography. Arrange five labelled vertical bands, left
to right, with the write path flowing down through them.
Band 1: one box, "API client".
Band 2, group border labelled "ltap-service (edges only)": two boxes, "POST /onboarding-submissions —
Idempotency-Key" and "Modulith modules: party / relationship / contactpoint / onboarding".
Band 3, group border labelled "core (JDK only)": three boxes, "OnboardingService — one unit of work",
"OnboardingPolicy — INV-1 / INV-2 / INV-3", "Ports: OnboardingStore, OnboardingTx,
OnboardingLakehouse".
Band 4, group border labelled "adapter-iceberg": one box, "IcebergOnboardingStore — raw Iceberg Java
API".
Band 5, group border labelled "docker compose": two boxes, "Iceberg REST catalog — atomic snapshot
pointer" and "MinIO — S3-compatible object storage, Parquet + metadata".
Arrows: one downward chain, client → edge → core → adapter, with no arrow pointing back up. From the
adapter box, two labelled arrows: to the catalog, labelled "commitTransaction: one catalog action",
and to MinIO, labelled "S3FileIO". A dashed line joins the catalog and MinIO, labelled "atomic
snapshot pointer".
Bottom right, reached from MinIO by one arrow: a separate box, "Analytical read — time travel, schema
evolution, projected scan".
In the margin, a faint crossed-out database cylinder labelled "no database in the write path".
Palette: accent blue for the write path, accent green for the read path, neutral grey for
infrastructure; colour always repeats a text label.
Render only the labels above: no extra text, no watermark, no invented boxes.
```

Three things about that picture are load-bearing.

**The write path never touches a database.** The adapter writes Parquet files to object storage and hands the
catalog a list of `TableCommit`s. The catalog is the only component that decides the winner. There is no lock
manager and no two-phase commit across systems.

**The read path is the same storage.** An analytical read pins a snapshot, projects and filters through
Iceberg's own scan API, and reads the Parquet files directly — this adapter is copy-on-write, so a snapshot is
data files alone, where a merge-on-read table would fold delete files in as well. Nothing is copied, transformed,
or re-indexed for analytics; a query for customers by relationship type as of a snapshot is a query over the same
bytes a transactional read sees.

**Failure is visible, not silent.** The edge refuses at startup if the catalog is unreachable — an edge that
accepts a submission it cannot commit is worse than one that never came up. A refused commit surfaces as an
explicit `409`, not as a retry that quietly applies itself on a newer base.

The step-by-step path of one submission through those layers is enumerated in §5.

---

## 3. The four concepts that do the work

Four ideas explain almost everything this architecture does and does not guarantee. They are worth five
minutes, because the failure modes are all consequences of them.

### 3.1 An open table format stores data as files plus a manifest

An Iceberg table is a directory of Parquet data files plus a tree of metadata: a `metadata.json` per version,
an Avro manifest list, and manifest files that name the live data files for one snapshot. A "write" produces
new files; a "commit" produces a new metadata version. Because the data files are immutable and the metadata
is append-only, history is a by-product of writing, not a feature bolted on. This is what makes time travel
and audit the same mechanism — for as long as retention keeps it: snapshots and old metadata files expire by
default (`history.expire.max-snapshot-age-ms`, `write.metadata.previous-versions-max`).

The corollary matters: on object storage there is **no atomic rename**. A commit cannot be "swap the file".
The catalog has to be the one that decides which metadata version is current, atomically.

### 3.2 Isolation is snapshot, and snapshot is not serializable

Iceberg commits are optimistic: a writer reads a snapshot, prepares new files, and the catalog accepts the
commit if the base it read is still current. Conflict detection is coarse: the unit is the table's **branch
ref** plus the files a commit touches — never rows, never partitions. A commit that asserts the ref refuses
*any* concurrent commit on that table; a commit that may retry (the default) rebases onto the new base and
lands. Which one you get is a table property, not a fact about the writers.

That is snapshot isolation, and it is exactly what allows **write skew** — the anomaly at the heart of this
work. Two transactions each read a count, each conclude a rule is satisfied, and each write a *different*
row. Their file sets are disjoint, so the loser's retry rebases cleanly onto the winner's snapshot, both
commits land, and the rule they both verified is broken by their combined effect. The repository's first use
case makes this concrete with signatories:

```java
public static boolean revoke(Tx tx, String partyId, String signatoryId) {
    if (tx.countAuthorized(partyId) <= MIN_AUTHORIZED) {
        return false;   // refuse: would leave the party with no authorized signatory
    }
    tx.setAuthorized(partyId, signatoryId, false);
    return true;
}
```

Each transaction reads "2 authorized" and clears a different row. Both commit. The party ends with zero
authorized signatories — a state no serial order of those two transactions could have produced. **Changing
the storage engine, and getting real ACID on object storage, did not change this.** It is a property of the
isolation level, not of the database.

The mitigation is not engine configuration; it is a *serialization point*: give the transaction something
physical to collide on. A per-party counter, written by both transactions, turns a disjoint-write race into a
write-write conflict the catalog can refuse.

### 3.3 A commit requirement refuses to overwrite a moving base

When a writer commits, it does not merely say "here are new files". It asserts the base snapshot it read,
through a requirement such as `UpdateRequirement.AssertRefSnapshotID`. If the base moved — because another
writer committed in the meantime — the requirement fails and the commit is refused instead of being silently
merged on top of a state the writer never saw.

That is the whole safety story: the mechanism that protects the count/absence invariant is the writer's own
requirement on the snapshot it read. And the *retry policy* is part of the isolation you get:
`commit.retry.num-retries` (default `4`) is what makes a client transparently re-apply a refused commit on the
new base, which converts a refusal into a lost update. This repository sets it to `0` on the counter tables to
buy a compare-and-set instead of a convergence. Retry is a policy, and it decides whether you get an abort or a
corrupted invariant.

### 3.4 The catalog makes the atomic decision; object storage provides durability

Split the roles and the design gets simple. The catalog owns the single atomic decision — *which snapshot is
current* — and object storage owns durability and cheap scale. Neither is asked to do the other's job. This is
also why the catalog has to be real: a mocked catalog would test the mock's isolation, not the lakehouse's.

---

## 4. Key designs, in depth

Four designs carry the architecture. Each one exists because a simpler version was tried or rejected.

### 4.1 The multi-table commit: one unit of work, one catalog action

This is the design that makes LTAP work for an aggregate that spans tables. The atomicity does not come from
ordering commits — no ordering of per-table commits can be safe when a writer may die between them. It comes
from asking the catalog to apply them together:

```java
// elided excerpt — IcebergOnboardingStore.commit()
List<TableCommit> commits = new ArrayList<>();
for (OnboardingTable type : OnboardingTable.values()) {
    TableCommit tableCommit = stage(type);
    if (tableCommit != null) {
        commits.add(tableCommit);
    }
}
committer.commitTransaction(commits);        // one catalog action for the whole unit of work
```

`TableCommit.create(...)` derives an `UpdateRequirement.AssertRefSnapshotID` requirement per table, so a
concurrent writer on *any* touched table is refused rather than quietly merged on top of — stricter than the
optimistic merge a plain `table.newAppend()` performs. On a catalog that implements the transaction endpoint as
one action (Nessie, §10), a relationship can never reference a party from a different unit of work, and a writer
that dies mid-submission leaves nothing behind. Two scope notes: the client cannot verify *how* the catalog
applied the batch, so "nothing at all was applied" is the adapter's reading of a refusal rather than something
it observes; and on the 1.10.1 reference fixture the guarantee does not survive concurrency — §7.1 records the
`LOST UPDATE` row that proves it.

> **Figure 3 — One unit of work, one catalog action.** *One `TableCommit` per touched table, pinned to the
> snapshot it read, converges into a single `commitTransaction` that a catalog applies all-or-none. Whether the
> call itself is one atomic action is the catalog's to guarantee — and §7.1 measures the fixture failing to.*

**Prompt for Gemini:**

```text
Figure 3 — One unit of work, one catalog action

Vertical UML-style sequence diagram, 16:9 landscape, white background, flat vector, clean sans-serif
type, no 3D, no photography.
Five lifelines across the top, left to right, each a labelled header box above a thin dashed vertical
line: "HTTP edge", "OnboardingService", "OnboardingTx (buffered)", "Iceberg REST catalog", "MinIO S3".
Then, in this exact order, horizontal arrows with these exact labels:
1. edge → OnboardingService, solid arrow, "onboard(submission)"
2. OnboardingService → OnboardingTx, solid arrow, "inTransaction(policy + writes)"
3. a note box over the OnboardingTx lifeline, "pin a base snapshot per table, then buffer rows in
memory"
4. OnboardingTx → MinIO S3, solid arrow, "write new Parquet files"
5. OnboardingTx → Iceberg REST catalog, solid arrow, "commitTransaction(List of TableCommit)"
6. a note box over the catalog lifeline, "assert every table's base snapshot, then publish the batch"
7. catalog → OnboardingService, dashed return arrow, "applied, or refused"
8. OnboardingService → edge, dashed return arrow, "Onboarded or Rejected"
Enclose messages 3 to 6 in one pale highlight rectangle labelled "one unit of work", so the batch
reads as a single step, and place a small footnote beside message 7: "one atomic action is the
catalog's to guarantee".
Palette: dark grey text, blue arrows for calls, dashed grey arrows for returns, one pale amber
highlight for the unit of work. Every lifeline and message carries its label; no other text.
```

### 4.2 The serialization point, and when it is redundant

Snapshot isolation gives you no help with a count/absence predicate. The design answer is to manufacture a
conflict the engine *can* see: a per-entity counter written by every transaction that reads the count.

```java
public static boolean demoteGuarded(OnboardingTx tx, String partyId, String contactPointId) {
    tx.touch(partyId);              // the serialization point: both writers touch the same key
    return demote(tx, partyId, contactPointId);
}
```

The interesting part is where it is *needed* and where it is not, and the harness measures both. On the
in-memory reference store, `touch` is load-bearing: without it, two concurrent demotions both pass and the
invariant breaks. On the lakehouse, the multi-table commit's own requirement on the snapshot is already
strict enough that `touch` is **redundant** — the losing commit is refused either way.

That is not a wart; it is the point of driving *one* port against *two* engines. "Safe by design" and "this
engine happens to refuse everything" look identical in a single-engine table. Two engines, two mechanisms,
opposite conclusions — and now you know which part of your safety is the engine's and which part is yours.

---

### 4.3 Three invariants, three anomaly classes, three mechanisms

The design discipline that makes this architecture legible is that **every invariant names the anomaly class
it belongs to and the mechanism that protects it**. If you cannot name the class, you cannot know whether your
protection is real or decorative.

| # | Invariant | Anomaly class | Protected by | Status |
| --- | --- | --- | --- | --- |
| INV-1 | a party keeps at least one **primary** contact point | count / absence (write skew) | the commit's requirement on the `contact_point` snapshot it read | measured — sequenced interleaving; the overlapping run is the recorded failure `F-4` (§7.1) |
| INV-2 | every relationship and contact point references a party written by the same unit of work | cross-table reference (half-write) | the same commit publishes both tables or neither | measured — crash test, i.e. a writer that dies before the commit; the concurrent case is `F-4` (§7.1) |
| INV-3 | `tax_id`, and `(channel, value)`, are unique | phantom / duplicate | the same requirement mechanism on the scanned table's snapshot | reasoned on the lakehouse (same mechanism as INV-1, so the same `F-4` caveat), measured on the reference store |

> **Figure 4 — Every invariant names its anomaly class and its mechanism.** *INV-1 maps count/absence to the
> commit's snapshot requirement; INV-2 maps a cross-table reference to one multi-table commit; INV-3 maps a
> phantom/duplicate to the same requirement mechanism on the scanned table.*

**Prompt for Gemini:**

```text
Figure 4 — Every invariant names its anomaly class and its mechanism

Three-column mapping infographic, 4:3, white background, flat vector, clean sans-serif type, no 3D,
no shadows, no decorative icons.
Column headers, aligned above their columns: "Invariant", "Anomaly class", "Protected by".
Left column, three rounded boxes stacked top to bottom: "INV-1 — a party keeps at least one primary
contact point", "INV-2 — every relationship and contact point references a party written by the same
unit of work", "INV-3 — tax_id, and (channel, value), are unique".
Middle column, three boxes aligned row by row: "count / absence (write skew)", "cross-table reference
(half-write)", "phantom / duplicate".
Right column, three boxes aligned row by row: "the commit's requirement on the contact_point snapshot
it read", "the same commit publishes both tables or neither", "the same requirement mechanism on the
scanned table's snapshot".
Draw one arrow per row, left to right, so the picture reads as three chains of
invariant → anomaly class → mechanism.
Under each row, in smaller regular type, add its evidence strip: row 1 "measured — sequenced
interleaving; the overlapping run is the recorded failure F-4", row 2 "measured — crash test: a
writer that dies before the commit", row 3 "reasoned on the lakehouse, measured on the reference
store".
Give each row its own tint — blue, amber, violet, top to bottom — and repeat the tint as a text
label, so colour is never the only signal. Render only the strings above; add no other text.
```

INV-1 is the direct successor to the isolation finding: the *same shape* of predicate — read a count, write a
row, the engine cannot see the predicate — on an entity that spans tables rather than one partition. That is
exactly why the mitigation had to move from "order two commits" to "make them one".

INV-2 is the invariant the single-table proof of concept never had to face, and it is the concrete reason this
became an architecture problem and not a table-renaming exercise.

INV-3 is where the honest gap lives, and the repository says so out loud: on the lakehouse it rests on the
same requirement mechanism as INV-1 rather than on a scenario of its own. Calling that "proven" would be the
kind of claim this whole approach exists to avoid.

### 4.4 Evidence is part of the design, not the appendix

The most reusable idea in this repository is not the commit call. It is the **latch harness**: a test that
forces *exactly one* interleaving — read-read, then write-write — using a `CountDownLatch`, so the race is
reproducible instead of flaky. A verdict is then computed from the observed end state:

- **Observed end state**, not an assertion about it. `final primary contact points = 1` is a measurement.
- **A verdict string derived from that state**: `PREVENTED`, `WRITE SKEW`, or — for the case where the catalog accepted both commits and a write vanished — `LOST UPDATE`.
- **A machine-readable table**, written to `target/evidence/verdict.md`, that a reader and a CI job consume the same way. A regression changes a row, not a comment nobody reads.

This is what turns a concurrency claim from a rumour into an engineering input. It is also what let the
architecture survive its own negative result: the write-skew row sits *in* the table, next to the mitigation
row, so nobody can quote one without the other.

### 4.5 Two catalog settings that decide whether the proof is real

The reference REST catalog the suite boots — `apache/iceberg-rest-fixture:1.10.1`, a REST server over a single
SQLite file rather than a catalog with a native multi-table commit — has two defaults that quietly invalidate a
concurrency PoC, and both are overridden in `docker/docker-compose.iceberg.yml` with the reason in a comment:

- `CATALOG_URI` — the image defaults to `jdbc:sqlite::memory:`, which hands **each pooled connection its own empty database**. Fine single-threaded; fatal the moment two commits race, with `no such table: iceberg_tables`. One real SQLite file, shared by every connection and opened with WAL and a busy timeout, is what makes the catalog's optimistic commit *actual*.
- `CATALOG_JDBC_STRICT__MODE=false` — restores the standard one-row-per-table `iceberg_tables` shape.

A test that cannot reproduce the contention it claims to test is worse than no test, because it produces
confidence. These two lines are the difference between measuring a catalog and measuring a mock.

---

## 5. Inputs, logic, and outputs

**Input.** One JSON submission, deliberately ordinary:

```json
{
  "party":        {"partyId": "P-100", "type": "INDIVIDUAL", "legalName": "Ada Lovelace",
                   "taxId": "TAX-100", "kycLevel": "TIER_1"},
  "relationship": {"relationshipId": "R-100", "type": "RETAIL_CLIENT", "status": "ACTIVE",
                   "effectiveFrom": "2026-09-27"},
  "contactPoints": [{"contactPointId": "C-100", "channel": "EMAIL",
                     "value": "ada@example.com", "primary": true}]
}
```

**Logic, in order.** The pipeline is short on purpose; everything interesting is in the rule or the commit.

1. **Validate the envelope.** A missing idempotency key or a malformed body is answered, not absorbed.
2. **Claim the idempotency key.** Same key and same payload: replay the first response verbatim. Same key with a different payload: `409`, because the client is confused about what it is asking for.
3. **Open one unit of work.** `OnboardingStore.inTransaction(...)` is the boundary; the adapter decides what "transaction" means.
4. **Apply the invariants.** INV-1 (at least one primary contact point — a count/absence predicate), INV-2 (every relationship and contact point references a party written in the same commit — a cross-table reference), INV-3 (`tax_id`, and `(channel, value)`, unique — a phantom/duplicate predicate). A refusal writes nothing.
5. **Stage and commit.** One `TableCommit` per touched table, each asserting the snapshot it read, handed to the catalog in one `commitTransaction` call.
6. **Map the result.** `Onboarded` → `201`; `Rejected(code, detail)` → an explicit code and HTTP status.

**Outputs.** A `201` that means "the use case ran and the commit was accepted" — a qualifier that matters,
because accepted is not the same as durable. `X-Idempotency-Status` appears on *every* response, valued `fresh`
or `replay`, so a first response and a retry differ by a value rather than by an absence — the harder thing to
test. A replay is byte-identical to the first response, status code included, because the body is rendered once
inside the claim and stored as the text the client received; re-rendering it on the way out would give the same
bytes today and a different status tomorrow.

Every way a submission can end in an error has exactly one status, one code, and one disposition:

| Raised by | Status | Code | Remembered? |
| --- | --- | --- | --- |
| a missing `Idempotency-Key` | 400 | `IDEMPOTENCY_KEY_MISSING` | there is no key to remember |
| a body that does not read cleanly | 400 | `MISSING_FIELD` / `UNKNOWN_VALUE` / `MALFORMED_FIELD` / `UNREADABLE_BODY` | no — the key is not reserved until the request reads cleanly |
| the key reused with a different payload | 409 | `IDEMPOTENCY_KEY_REUSED` | the earlier record stands untouched |
| the lakehouse already holds the identity | 409 | `DUPLICATE_PARTY` / `DUPLICATE_TAX_ID` / `DUPLICATE_CONTACT_POINT` | yes — a retry replays the same refusal |
| the payload contradicts itself | 422 | `NO_CONTACT_POINT` / `NO_PRIMARY_CONTACT_POINT` | yes — no other state is involved, so the payload itself must change |
| another writer moved the snapshot | 409 | `CONCURRENT_COMMIT` | **no** — see below |

That last row is the one worth designing for. A commit refused because another writer moved the snapshot is
the one failure a client *should* retry, so it deliberately escapes the claim as an exception: the key is
released, and the retry reaches the use case instead of being answered with a `409` for something that never
committed. The body says so in as many words — *"nothing was committed, so this submission may be retried as
it stands"*. Refusals replay; a failure to try does not.

Read the contract for what it is: the edge is a *mapping layer*. It holds no business rules, and it holds no
second copy of the invariants — a duplication that would drift the day someone changed a rule in `core` and
forgot the HTTP layer existed.

---

## 6. Key features, mapped to the repository

An architecture is only credible when you can point at the file that implements each claim. Every feature
below exists for a stated reason, and lives in a named place.

| # | Feature | Why it is there | Where it lives |
| --- | --- | --- | --- |
| 1 | Framework-free domain core | The invariant cannot see the engine, and the engine cannot see the invariants, so one rule is reused across two very different stores | `core` — `OnboardingPolicy`, `OnboardingService`, `SignatoryGuard` (JDK only; JUnit is test scope) |
| 2 | Ports and adapters at the boundary | The core declares *what* it needs and never *how* it is satisfied; no Iceberg, JDBC, or Spring type crosses the edge | `OnboardingStore`, `OnboardingTx`, `OnboardingLakehouse`, `OnboardingTable` |
| 3 | An open table format on object storage | Object storage's *lack* of an atomic rename is part of the problem under test, not an assumption hidden from it | `adapter-iceberg/.../IcebergOnboardingStore`, `docker/docker-compose.iceberg.yml` |
| 4 | A catalog as the atomic decision point | "Which snapshot is current" must be one atomic decision; the client never decides the winner | `RESTCatalog`, plus the fixture's two non-default settings (§4.5) |
| 5 | A cross-entity unit of work | Several tables publish in one catalog action, so an aggregate spanning tables is all-or-none — on a catalog whose transaction endpoint is one action, which §7.1 measures the reference fixture failing to be | `TableCommit` per touched table → `RESTCatalog#commitTransaction(...)`; `IcebergOnboardingStore` (§4.1) |
| 6 | The analytical read side | Snapshot pinning, time travel, metadata-only schema evolution, and a projected/filtered scan over the same files the write path produced | `TableScan.project(...).filter(...).useSnapshot(...)`; `OnboardingLakehouse` |
| 7 | Evidence as a first-class artifact | Every verdict is computed from the observed end state and written to a table, so a regression changes a verdict, not a comment | `e2e/.../ConcurrentDemote`, `EvidenceReport`, `e2e/target/evidence/verdict.md` |
| 8 | One port, two engines | A comparison is only meaningful if the *same* rule is driven against a second engine whose behaviour is known | `core`'s in-memory harness (republished as a test-jar) plus `IcebergOnboardingStore` |
| 9 | A build gate on the boundaries | "Core depends on nothing" is enforced, not trusted — and the gate is itself guarded against matching nothing and checking nothing | `CoreDependencyBoundaryTest` (ArchUnit) |
| 10 | A thin HTTP shell | The edge maps a request, claims a key, calls the use case, and maps the result — nothing more, because everything interesting lives in the rule or the commit | `ltap-service`: Spring Boot and Spring Modulith at the edges only, in four modules (`party`, `relationship`, `contactpoint`, `onboarding`) |

The dependency direction is the whole architecture in one line: `core ← {adapter-iceberg, service} ← e2e`, and
nothing points the other way. `e2e` deliberately does *not* depend on `service`; the edge is tested against
`core`'s in-memory harness, so there is no second fake to drift from the first.

> **Figure 5 — Dependency direction: nothing points back.** *`core` depends only on the JDK; `adapter-iceberg`
> and `service` depend on `core`; `e2e` depends on `core` and the adapter. No arrow points back toward `core`,
> and an ArchUnit rule fails the build if one appears.*

**Prompt for Gemini:**

```text
Figure 5 — Dependency direction: nothing points back

Flat vector dependency graph, 16:9 landscape, white background, thin strokes, rounded rectangles,
clean sans-serif type, no 3D, no shadows, no photography.
Four boxes: upper centre "core (JDK only) — rules + ports"; lower right "adapter-iceberg — Iceberg,
Parquet, S3FileIO"; to its right "ltap-service — HTTP edge, Spring Modulith"; bottom centre
"e2e — containers, latch harnesses".
Solid arrows, all pointing away from core, none pointing back: core → adapter-iceberg,
core → ltap-service, adapter-iceberg → ltap-service, core → e2e, adapter-iceberg → e2e.
Beside the core box, a small circled marker with a crossed-out reverse arrow, labelled "no arrow
points back".
Between ltap-service and e2e, a dashed grey line labelled "independent — the edge is not tested from
here".
On the core box, a small shield badge labelled "ArchUnit build gate".
Palette: core dark blue, adapter teal, service grey, e2e light grey, arrows dark grey; every colour
repeats its text label. Render only the labels above: no extra text, no watermarks.
```

---

## 7. What the architecture measures — and what it does not

Here is where the claim either survives contact with a running stack or does not. The full suite is green on
record against MinIO and the reference Iceberg REST catalog: **31 tests** (14 in `core`, 12 e2e scenarios
covering the isolation and the onboarding invariants, 5 in the onboarding-over-Iceberg suite), 0 failures — plus
22 tests on the HTTP edge that run without Docker. Every verdict below is computed from the observed end state.

### 7.1 The verdict table, abridged

| Engine | Scenario | Observed end state | Verdict |
| --- | --- | --- | --- |
| Iceberg on object storage | concurrent writers on one partition | 1 of 2 committed; the catalog refused the loser (`branch main has changed`) | **PREVENTED** |
| Iceberg on object storage | concurrent revocations of *different* rows | 2 of 2 committed; final authorized = 0 | **WRITE SKEW** — invariant broken |
| Iceberg on object storage | a compare-and-set counter closes the gap | 1 of 2 committed; final authorized = 1 | **PREVENTED** |
| Iceberg on object storage | the previous snapshot still holds the invariant | 2 of 2 committed; the pre-change snapshot reads authorized = 2 | **WRITE SKEW** — recorded; time travel reads the invariant intact |
| Iceberg on object storage | a column is added without rewriting a data file | metadata-only commit; new column, same files | **EVOLVED** |
| Iceberg on object storage | one scan answers the governance question across parties | one projected, filtered scan | **ANSWERED** — the analytical half |
| in-memory reference store | two concurrent demotions of different primaries | 2 of 2 committed; primaries = 0 | **WRITE SKEW** — INV-1 broken |
| in-memory reference store | the serialization point closes the gap | 1 of 2 committed; primaries = 1 | **PREVENTED** |
| Iceberg on object storage | the same interleaving vs the cross-table commit | 1 of 2 committed; primaries = 1 | **PREVENTED** |
| Iceberg on object storage | the two commits are allowed to overlap | 2 of 2 committed; one write vanished | **LOST UPDATE** — recorded, not asserted |

Two rows are worth reading twice.

**`WRITE SKEW`** is the finding, and it is *negative*: real ACID on object storage did not prevent it. That is
not a bug to fix; it is the boundary of what snapshot isolation promises, and it is the input to every design
decision above.

**`LOST UPDATE`** is the most honest row in the table. A `201` means "the use case ran and the commit was
accepted", which is *not always* the same as "the write is in the table". Root-caused and measured: the
reference catalog's multi-table commit is not atomic across concurrent requests, because its requirement check
and its metadata write are two steps (`F-4` in the repository's findings and ADR-009). It is not a client
artifact — the lost update reproduces at the same rate with two independent clients. Over 10 attempts each: when
the commits are sequenced the loser is refused **10/10** times and **0/10** updates are lost; when they overlap,
both commits are accepted and one write vanishes in **8/10** runs on one store and **7/10** across two
independent clients.

The suite therefore *names the commit order* for the rows it asserts, and **records** the overlapping shape
instead of asserting it. A limitation you can reproduce and cite is worth more than a green test that happens
to pass because the race rarely fires.

### 7.2 Measured, reasoned, or recorded

The discipline that keeps this article honest, and the one worth stealing: attach an evidence class to every
claim, and never let "green" and "reasoned" read the same in a table.

| Claim | Evidence class |
| --- | --- |
| A snapshot-isolated lakehouse exhibits write skew | **Measured** (signatory table, two engines) |
| The application-level serialization point works and is portable | **Measured** (one port, two engines) |
| The catalog's multi-table commit closes cross-table atomicity | **Measured** against the reference catalog when the commit order is named; **recorded failure** when two requests overlap (`F-4`); **reasoned** on Nessie |
| Time travel, schema evolution, and analytical scans over the same files | **Measured** through Iceberg's own scan API |
| The HTTP edge's idempotency and explicit refusals | **Measured** against `core`'s in-memory harness; the record is in-process |
| Concurrent overlapping `commitTransaction` is atomic | **Recorded failure** (`F-4`) — it is not, at the measured rates |

### 7.3 How the engines compare

| | MongoDB (snapshot) | CockroachDB (serializable) | Iceberg on object storage |
| --- | --- | --- | --- |
| Default isolation | snapshot | strict serializable | snapshot (optimistic commits) |
| Concurrent disjoint writes | both commit | both commit | **both commit** |
| Concurrent writes behind a shared read | both commit | one aborts | **both commit** |
| Cross-row invariant | broken | protected by the engine | **broken** |
| Mitigation | hot-row conflict | not needed | compare-and-set counter |
| Time travel / schema evolution | no | bounded historical reads (`AS OF SYSTEM TIME`, GC window); no open format | **yes** (open format) |

The two middle rows are the write-side story, and they are worth reading together: all three engines let two
disjoint writers through, and only the serializable one refuses anything when two transactions share a read —
which is exactly the difference between *serializable* and *snapshot*.

The column that changes the decision is the last one. A lakehouse earns its place on the read side — time
travel, schema evolution, scans over the same files — while behaving, on the write side, exactly like the
snapshot-isolated operational store beside it. LTAP is the trade that buys the read side without paying for a
second copy, and states the write-side cost in the same table.

---

## 8. Where this applies

LTAP is not for every workload, and the honest way to present it is by the shape of the *write*, not by
industry. It fits when the write side is a bounded unit of work over a small set of entities, and the read
side wants history.

- **Regulated onboarding and KYC.** A customer is several entities — the party, its relationships, its contact points — and they must become visible together. Compliance wants to read the state *before* a change, not just the state after it. That is a unit of work plus time travel, which is this pattern exactly.
- **Reference and master data management.** Party, product, account, and organisation hierarchies change rarely, are read constantly, and are audited relentlessly. Time travel over the same files answers "what did the hierarchy look like when we made that decision?" without a separate audit store.
- **Analytical workload on operational entities.** When the operational entity *is* the analytical subject — customer, account, policy, contract — the second copy exists only to give the warehouse something to scan. Removing it removes the ETL, the drift, and the freshness SLA in one move.
- **Common Data Model (CDM)–aligned data platforms.** Entity-relationship models with a small set of core entities and cross-entity invariants map naturally onto a cross-table commit, because the unit of work *is* the aggregate.
- **Facts with relationship edges.** Wherever a fact and its edges must not disagree — a relationship that references a party that must exist — INV-2's mechanism serves directly, whether the facts are event-grain or entity-grain.

And where it does **not** fit, which matters just as much:

- **High-contention hot rows** — counters, balances, inventory — where optimistic concurrency means constant refusals. A serializable OLTP engine is simply better at that, and no architectural pattern changes the physics.
- **Complex multi-statement transactions** with reads that span many tables and depend on each other. The more predicates your transaction needs to see, the more you are asking snapshot isolation to do something it does not do.
- **Workloads that need a mature query engine and BI today.** The read side here is Iceberg's own scan API; a production deployment wants Trino or DuckDB over the same files, and predicate and aggregation pushdown is a real gap in this proof.

---

## 9. Why LTAP, and not the alternatives

The usual answers to the two-copy problem each move it rather than solve it:

| Approach | What it costs | Where it breaks |
| --- | --- | --- |
| Batch ETL into a warehouse | A nightly pipeline and a freshness SLA nobody meets | Analytics is a day behind; backfills are migrations |
| CDC into Iceberg (Postgres → Debezium → Kafka → Flink) | Three more components to operate, plus streaming lag | Correct, but "transactional processing via Iceberg" is now four systems wide |
| Managed warehouse (Snowflake / BigQuery) | Lock-in, and no local reproduction | The mechanism is unattributable — you cannot show *why* it is safe |
| No isolation at all (`INSERT OVERWRITE` over Parquet) | Cheapest to write | No atomic commit, so no correctness story at all |

Two of those rejections are worth stating in full, because they are the ones a reviewer will push on.

**Ordering per-table commits in the application is the trap**, and the isolation finding walks straight into
it. Ordering two commits can rescue an invariant *confined to a single table*, because the losing commit can be
made to conflict. It cannot rescue a cross-table reference: no ordering makes two separate commits atomic, and
a writer that dies between them leaves an edge pointing at a row that does not exist. The design moved from
"order them" to "make them one", and that move is the architecture.

**A managed warehouse** has real ACID and no local story, so the mechanism is unattributable: you cannot show
a colleague *why* two concurrent writers behave as they do, because the locking, the commit protocol, and the
isolation level are not yours to inspect. A local, reproducible stack with an evidence table is a different
kind of asset — an argument you can hand to a security review.

CDC into Iceberg deserves the credit it gets: for a regulated system of record it is often the right answer.
LTAP's counter-proposal is narrower. For a bounded write-side workload the lakehouse itself can hold the
transactional copy — and the repository *measured* what that costs (you manage the serialization point
yourself) rather than asserting it was free.

LTAP's honest scorecard: it wins on **one copy**, on **history as a by-product**, and on **reproducibility**,
and it loses on **write-side isolation**, on **write latency under contention**, and on **operational
maturity**. The trade-offs table in §10 states both columns in full. A reader who cannot state both has not
evaluated it.

---

## 10. Closing notes

### The trade-offs, in one block

| You gain | You give up |
| --- | --- |
| One physical copy — no second store, no ETL, no freshness SLA | Snapshot isolation, not serializability: an application-level serialization point is yours to write |
| History as a by-product — time travel and audit over the same files | A catalog is now a required component, and it is on the write path |
| A commit protocol you can inspect and reproduce locally | Under contention, optimistic commits mean refusals the caller must handle |
| Schema evolution that costs metadata, not rewrites | Compaction and retention are real operational work this proof does not do |
| An evidence table instead of a concurrency claim | A `201` is "accepted", not "durable" — measured, and recorded as a limitation |

### What is deliberately absent

The repository is explicit about its own boundary, which is a feature of the writing if not of the code:

- **Nessie is reasoned, not exercised.** The cross-table mechanism was measured against the reference REST catalog; a production deployment would point the same adapter at a catalog with multi-table commits as a first-class, committed feature. Until that is run, the claim is *reasoned*, and the table says so.
- **The CDC path is documented, not built.** For a regulated system of record it is often the right answer, and it is described as the alternative architecture — with the trade-off table — rather than hidden.
- **No query engine, no compaction, no retention, no auth.** The read side is Iceberg's own scan API; the stack is single-node, plain HTTP, with default credentials, for local use only.
- **No performance numbers.** This proof is about isolation behavior, not throughput; conflating the two would weaken both.

Naming these is not an apology. It is the same discipline as the verdict table: an architecture you can
describe the edges of is one you can adopt with your eyes open.

### Two questions readers ask

**"Isn't this just Iceberg with extra steps?"** Iceberg gives you atomic commits, time travel, and schema
evolution — necessary, but not sufficient. LTAP is the set of decisions *on top* of the table format: which
unit of work the domain exposes, where the serialization point lives, how a cross-entity invariant is
protected, and how you prove any of it. The format is the substrate; the architecture is the part that decides
whether your invariants survive a race.

**"If snapshot isolation breaks my count invariant, why not just use a serializable database?"** For
high-contention hot rows, often you should: serializable OLTP is simply better there, and this article says so
above. The question LTAP answers is different. When your analytical copy already exists and your write side is
a bounded unit of work over a few entities, can one copy serve both, and what exactly does that cost? The
repository's answer is a qualified yes, with the qualification measured rather than glossed.

### Final thought

The most valuable thing this architecture produces is not a multi-table commit call. It is a habit: name the
invariant, name the anomaly class, name the mechanism, and then **measure whether the mechanism actually
fires** — and publish the row where it does not.

A lakehouse will happily tell you it committed. LTAP is the discipline of asking it what it committed, from
which base, with what evidence, and writing the answer down where a reviewer can check it.

