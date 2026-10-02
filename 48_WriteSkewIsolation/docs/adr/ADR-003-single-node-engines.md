# ADR-003: Single-Node Engines — Isolation, Not Topology

## Problem statement
Must the PoC run a sharded MongoDB cluster and a multi-node CockroachDB to make the architecture
point?

## Background
The question being answered is whether isolation level or topology determines write-skew behaviour.
A sharded topology scales writes but does not change the isolation level, so it is irrelevant to the
phenomenon under test. The PoC therefore uses one replica-set member and one single CockroachDB node.

## Constraints
- Reproduce the isolation behaviour only.
- Keep the environment cheap and deterministic (single-node, local Docker).
- Exclude sharding, geo-partitioning, HA, latency, residency, and TLS.

## Assumptions
- MongoDB transactions (and thus snapshot isolation) work on a single-member replica set `rs0`.
- A single-node CockroachDB still enforces strict serializable (SSI) locally.

## Options

| | Single-node engines | Sharded Mongo cluster | Multi-node CockroachDB | Full geo-distributed |
| --- | --- | --- | --- | --- |
| Summary | One replica-set member + one CRDB node | Real sharded topology | 3-node CRDB | `REGIONAL BY ROW` across regions |
| Isolates the isolation variable | Yes | No — adds topology noise | No — adds HA noise | No |
| Cost / determinism | Low / high | High / lower | High / lower | Highest / lowest |
| Score (out of 5) | 5 | 2 | 2 | 1 |
| Remarks | Chosen. | Rejected: proves nothing extra for the cost. | Rejected: HA is out of scope. | Rejected: contradicts scope. |

## Consequences
- Write skew is demonstrated as an *isolation* property, not a *sharding* property.
- The result extends by argument to a sharded cluster (see
  [ADR-006](ADR-006-sharding-does-not-change-outcome.md)) without paying for one.
- The environment stays reproducible on a single developer machine.
