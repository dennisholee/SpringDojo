# Assessment — Does This Repo Realise the LTAP Architecture?

> **Status (2026):** a close-out read of Phases 1–4 against the thesis the repo claims to
> demonstrate. Its purpose is to attach an **evidence class** to every row a reader meets in the
> README's verdict table, so "green", "IT-backed" and "reasoned" are not read as the same thing.
> It adds no code to the PoC; the two things it does add are measurements — the baseline run below and
> F-4's root cause — not assertions. Where it qualifies a claim it says which measured fact does the
> qualifying.
>
> **Baseline run (2026-09-27):** the full suite is green on record — `mvn -pl e2e -am -Pe2e test`,
> 31 tests (core 14, `LtapE2EIT` 12, `OnboardingOnIcebergIT` 5), 0 failures, against MinIO and the
> 1.10.1 reference REST catalog. That clears the measurement caveat that used to hang over O3–O5. Two
> qualifications survive it, both by design rather than by omission: **O6 remains a recording** (it
> pins only the invariant), and **Nessie remains unexercised**.

## The question

The repo states the LTAP shape itself (README §3): *one physical copy of the rows, transactional
enough to write to, analytical enough to reason over, and versioned enough to audit.* ADR-010 splits
that into two architectures — **A** (OLTP-front + CDC) and **B** (direct-write lakehouse with a
cross-table catalog) — with **Hybrid** as the standing recommendation. So "realising LTAP" is two
independent questions, and they have different answers:

1. **Is the thesis proven?** A lakehouse's ACID is snapshot isolation, so a count/absence invariant
   is still exposed to write skew, and the mitigation is an application-level serialization point.
2. **Is the write path realised?** Path B actually publishes a multi-entity unit of work
   transactionally, and the same files answer analytical reads.

## Verdict against the four claims

| # | Claim the repo makes | Rows / artifacts | Verdict |
| --- | --- | --- | --- |
| a | The snapshot-isolation anomaly **exists** on a real lakehouse | L2, L4 (signatory); O1 (reference); O6 (lakehouse, recorded) | **Proven** for the signatory table; **proven on the reference engine** for onboarding; the lakehouse's own violation is **recorded, not asserted** (O6 / finding 6) |
| b | The **application-level mitigation** works and is portable | L3 (signatory); O2 (reference, re-verified green); O4 (lakehouse) | **Proven** across two engines through one port; both halves green in the same full run (2026-09-27) |
| c | The **catalog's multi-table commit** closes cross-entity atomicity | O3, O5; `OnboardingOnIcebergIT` crash test | **Demonstrated** against the reference catalog — green in the full run and in the IT; **not measured on Nessie** — the ADR-009 recommendation is reasoned |
| d | **Time travel / schema evolution / analytical scans** over the same files | L4, L5, L6 | **Proven**, but through Iceberg's own scan API in-process; no query engine, and aggregation pushdown is out of scope (ADR-006) |
| — | The **REST edge** (idempotency, explicit refusals) | 22 `ltap-service` tests | **Proven** against `core`'s in-memory harness; the idempotency record is in-process |

## Measured, reasoned, or recorded

| Surface | Evidence class |
| --- | --- |
| L1–L6 (signatory) | **Measured** on Iceberg + MinIO + the reference REST catalog; green in the 2026-09-27 full run |
| O1–O2 (onboarding, reference) | **Measured and re-verified green** after the commit-order change — no container needed |
| O3–O5 (onboarding, lakehouse) | **Measured** — green in the 2026-09-27 full run, and independently pinned by `OnboardingOnIcebergIT` |
| O6 (onboarding, overlapping) | **Recorded, not asserted** — it declares only the invariant, so a changed verdict is a finding about the engine, not a broken build. The 2026-09-27 run reproduced `LOST UPDATE` (F-4) |
| Nessie as the production catalog | **Reasoned, not measured.** ADR-009 is explicit: the mechanism is the catalog's REST transaction endpoint, Nessie is its production home, and the PoC measures the reference catalog |
| REST edge | **Measured** (in-process harness) |

## Open findings and deferrals

- **F-4 (root-caused, not fixable here — ADR-009)** — when two `commitTransaction` calls were in flight at
  once, the 1.10.1 reference catalog accepted **both** and one demotion vanished silently, the
  `party_counter` increment with it; INV-1 survived only because each snapshot is a full replacement built
  from the same base. **Root cause measured 2026-09-27: the catalog's multi-table commit is not atomic
  across concurrent requests.** The client-side alternative (a shared cached `Table`) is refuted —
  `RESTCatalog.loadTable` has no cache, and the lost update reproduces with two independent clients at the
  same rate. Over 10 attempts each: sequenced **0/10** lost updates (the loser is refused every time),
  overlapping **8/10** one store, **7/10** two clients. Not fixed, and not fixable from this repo: it is the
  reference fixture's server-side behaviour, so the disposition is "record it, do not vendor a fix".
  **Consequence: a `201` means "the commit was accepted", which is not always "the write is in the table"**.
- **Nessie is documented, not verified** — one of the two catalogs the recommendation names has not
  been exercised. ADR-009 already records that the *native* `NessieCatalog` client does not expose
  the endpoint; the REST path against a Nessie server has not been run.
- **Idempotency is in-process** — a retry that crosses a restart is caught by INV-3 (duplicate tax
  id) rather than replayed. The durable version belongs in the same multi-table commit; the
  mechanism is present, the table is not.
- **Orphaned Parquet on abort** — `stageOnly()` + `writeFile` can leave data files behind when a
  transaction is refused. Storage hygiene, not correctness.
- **Closed: the full suite is green on record** — the 2026-09-27 run (31 tests, 0 failures) supersedes
  the earlier partial one (`Tests run: 16, Failures: 3`) that predated the commit-order change. What had
  blocked it was not `containerd` but the engine itself: the Docker VM had stopped, and the Desktop app
  was up without it. Restarting the engine and re-running cleared the item, so O3–O5 read as **measured**
  rather than IT-backed-and-pending.

## Gap → effort → value

Effort is T-shirt sized; value is judged against the two questions above (thesis vs write path).

| # | Gap | Effort | Value | Why now / why later |
| --- | --- | --- | --- | --- |
| 1 | ~~Restart Docker and re-run the full e2e suite~~ **closed 2026-09-27** | S | **High** | Done: 31 tests, 0 failures. O3–O5 are measured; O6 and Nessie keep their caveats on purpose. It was the cheapest action with the largest honesty gain |
| 2 | ~~Root-cause **F-4**~~ **root-caused 2026-09-27** | M | **High** | Done: the catalog's commit is not atomic across concurrent requests (sequenced 0/10, overlapping 8/10 one store, 7/10 across two clients), and the client-side hypothesis is refuted. Not fixable in-repo — it is the reference fixture's server behaviour |
| 3 | Exercise the multi-table commit against **Nessie** | S–M | Med-High | Closes the only gap between ADR-009/010's recommendation and measured evidence, on the stack the ADR names |
| 4 | ~~**T003-010** — rewrite README/DESIGN around onboarding~~ **closed 2026-09-27** | M | Medium | Done: `DESIGN.md` is now a two-layer document (§1–§8 the finding, §9–§10 the solution, §11–§12 shared) rather than a PoC carrying a scope note. It was the last document that read as though the PoC were still the current design |
| 5 | **Durable idempotency** (a fifth table in the same commit) | M | Medium | Removes the restart caveat; the mechanism already exists in the adapter |
| 6 | Orphaned Parquet on abort | S–M | Low-Med | Storage hygiene. F-4 is now root-caused and recorded rather than fixed, so this is the cheapest remaining write-path item |
| 7 | **T003-009** — demonstrate the CDC path (A) | L | Medium | Fulfils FR-003-007 and completes the A/B recommendation; **not** required for the isolation thesis |
| 8 | Operational half — compaction, retention, security, query engine | L+ | Out of PoC scope | ADR-006 names each as its own experiment; the PoC is deliberately not a production reference |

## What a skeptic would say

- **The lakehouse's own invariant violation is the weaker half.** O1 — the write skew the onboarding
  use case is *about* — runs on `core`'s in-memory reference store, not a real engine. On the
  lakehouse the violation is only O6's **accidental** survival, which is a recording, not an
  assertion. A real-engine write-skew, if constructible, would materially strengthen the claim; if it
  is not constructible, that itself is a finding worth stating.
- **"Cross-table atomicity" is measured on the reference catalog and recommended on Nessie.** The
  two are not the same server, and ADR-009 says so. Until row 3 above is run, the mechanism is
  measured and the product choice is reasoned.
- **A `201` is not a durability guarantee** (finding 6 / F-4). Read the edge's contract as "the use
  case ran and the commit was accepted", not "your write is in the table".
- **Single-node, plain HTTP, `minioadmin`, no auth, no performance numbers** — ADR-006 lists all of
  it, so it is disclosed rather than hidden, but it is disclosed for good reason.

## Bottom line

- **As a proof of concept of the thesis**, the repo succeeds, and its evidence discipline is the
  strongest part of it: one port, one latch harness, two engines, verdicts computed from observed end
  state, boundary gates that bite, and findings recorded instead of smoothed over.
- **As a realisation of an LTAP architecture**, it is a *scaffold with a proof underneath*: the
  transactional write path (B) is built and measured; the operational half (A / CDC, a real query
  engine, compaction, retention, security) is documented and deliberately absent; and one open
  finding (Nessie) plus one root-caused but unfixed limitation (F-4) stand between "measured" and
  "settled".

**Recommended order:** rows 1, 2 and 4 are closed. Next is row 3 (**Nessie** — the last reasoned statement
in the table), then rows 5–7. F-4 stays a recorded limitation rather than a task: it is the reference
fixture's server behaviour, and vendoring a fix for a fixture is not this repo's job.
