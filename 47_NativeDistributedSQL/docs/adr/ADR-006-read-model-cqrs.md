# ADR-006: Read Model for Customer 360

## Problem statement
How should a cross-region customer-360 view be served without paying cross-region ACID, or a four-table join, on every read?

## Background
Serving 360 from the write model means joining party, relationship, contact_point and product_holding, and two of those tables live in different regions. The PoC (`RM`) maintains a denormalised `customer_360` table outside the write path: scenario RM-1 asserts the view returns 404 immediately after a write, then converges once the projector runs. An ArchUnit rule forbids the write path from depending on the read model.

## Constraints
- The write transaction must not be coupled to the projection.
- Eventual consistency is acceptable for this view.
- No application outbox, per ADR-004.
- The view must be readable from either region.

## Assumptions
- A bounded staleness window is acceptable to consumers of the view.
- The projected row count fits in a single table.
- Reads outnumber writes, so a projection pays for itself.

## Options

| | Projected read model | Synchronous join | No read model |
| --- | --- | --- | --- |
| Summary | Denormalised table maintained outside the write path | Join the four tables on each read | Consumers query the source modules themselves |
| Description | A projector reads the source tables and upserts one flat row per party. Reads hit only the projection. | Each 360 request joins party, relationship, contact_point and product_holding in one query at request time. | Callers assemble the view from separate module APIs. |
| Pros | Reads are single-row and region-local; the write path is untouched; the projection can be rebuilt. | Always current; no projection code or storage. | No new component. |
| Cons | Eventually consistent; a projector to run and monitor. | Cross-region join cost on every read; the read shape leaks into the write model. | Every consumer reimplements assembly; inconsistent results between consumers. |
| Score (out of 5) | 5 | 3 | 1 |
| Remarks | Chosen. RM-1 disproves the tempting shortcut of updating the projection in the write transaction. | Acceptable only if read volume is low and latency is not a concern. | Rejected: 360 is a stated platform capability. |
