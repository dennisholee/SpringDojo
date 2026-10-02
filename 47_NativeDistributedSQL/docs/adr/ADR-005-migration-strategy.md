# ADR-005: Migration Strategy Off MongoDB

## Problem statement
How do we move the customer master off MongoDB without a cutover that loses or duplicates customer data?

## Background
The legacy store holds one document per customer, embedding the party, its relationship, its contact point and its product holding. The PoC (`MIG`) implements a backfill sweep, a strangler dual-write, and reconciliation from the MongoDB change stream using a persisted resume token, always followed by a full sweep so convergence never depends on the stream being available. The remaining difference between the two stores is exposed as a single gap value that must read zero before cutover.

## Constraints
- The legacy store stays the system of record until cutover; the customer master has no read downtime.
- No transactional outbox in the target, which ADR-004 rules out.
- Reconciliation must be idempotent: replaying it must not create duplicate rows.

## Assumptions
- Legal name is the natural key between the two stores for the PoC.
- The legacy document maps one-to-one onto the four normalised CDM tables.
- Change streams are available, which requires the legacy store to run as a replica set.

## Options

| | Dual-write + change-stream reconcile | Big-bang cutover | CDC push only | Shadow and verify |
| --- | --- | --- | --- | --- |
| Summary | Write to both stores; reconcile deltas until the gap is zero; then cut over | Freeze, export, import, switch | Legacy emits every change to the target | Serve reads from the target while the legacy store stays authoritative |
| Description | New writes go to the CDM and are mirrored into the legacy store. Backfill imports history. A reconciler drains the change stream and sweeps, reporting the gap. | A maintenance window stops writes, a one-off export/import runs, and the target is switched on. | The legacy store publishes changes; the target consumes and applies them. No backfill sweep. | Reads are dual-run and compared; writes still go to the legacy store only. |
| Pros | No freeze; incremental and reversible; gap is measurable; each customer's write is atomic. | Simplest mechanism; one cutover event. | Near-real-time; no periodic sweep. | Proves read equivalence with zero write risk. |
| Cons | Two write paths to maintain during the transition; needs a resume token and an idempotent sweep. | Requires downtime; failure mid-cutover is hard to unwind; no rollback once writes resume. | Ordering, replay and poison-message handling are all in scope; history still needs a backfill. | Does not migrate writes, so it cannot complete the cutover alone. |
| Score (out of 5) | 5 | 2 | 3 | 3 |
| Remarks | Chosen. `@PoC-MIG` asserts backfill, dual-write, reconciliation and idempotency all end at gap zero. | Rejected: the risk profile is unacceptable for the customer master. | Viable transport, but needs the backfill and idempotency of the chosen option anyway. | Useful as a safety net layered on the chosen option, not as a strategy. |
