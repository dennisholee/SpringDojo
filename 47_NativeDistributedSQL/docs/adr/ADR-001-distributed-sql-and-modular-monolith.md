# ADR-001: Replace Per-Region MongoDB with Geo-Partitioned Distributed SQL

## Problem statement
MongoDB replica sets are deployed per region, so no transaction can span UK and HK. Cross-region consistency across the system services is therefore built in the application as Saga / TCC / outbox compensation, and the three-layer split multiplies the round trips. Composite CDM operations are both slow and complex.

## Background
Core domains (Party, Relationship, Contact Point) are mastered in the UK; Product Holding is mastered in the HK market. A single onboarding touches both, so the application has to coordinate them. This PoC replaces the transactional core with a geo-partitioned distributed SQL database and collapses experience / process / system into one modular monolith, validated by an end-to-end suite: `A1`, `A2`, `B1`-`B4`, `R1`, `M`, `J`, `MIG`, `RM`.

## Constraints
- Data residency: UK data stays in the UK, HK data in HK.
- No application compensation: no `/try`, `/confirm`, `/cancel` endpoints and no outbox table.
- Strict serializable isolation for every operation.
- Self-hostable engine; no single-cloud lock-in.

## Assumptions
- The transactional core can be modelled relationally.
- MongoDB is retained only for genuinely document-shaped, non-transactional concerns.
- A third region, if needed for region survival, would hold replicas but no customer data.

## Options

**Storage and application shape**

| | Distributed SQL + modular monolith | Distributed SQL, three layers retained | MongoDB per region + formalised Saga/TCC | Two independent clusters + async sync |
| --- | --- | --- | --- | --- |
| Summary | One geo-partitioned database; one deployable of domain modules | One geo-partitioned database; experience / process / system kept | Keep the document stores; make the compensation logic a first-class component | Separate single-region clusters reconciled in the application |
| Description | Rows are homed per region with `REGIONAL BY ROW`; the engine commits cross-region transactions via Raft. Experience / process / system collapse into one Modulith deployable. | Same storage change, but composite operations still traverse three layers and remain separately deployed. | Adds a durable compensation engine, idempotency keys and reconciliation to every cross-region operation. | Each region runs its own cluster; cross-region consistency is an application concern. |
| Pros | Native distributed ACID; one in-process transaction per composite operation; residency by policy; compensation code disappears. | Storage problem solved while deployment topology stays unchanged. | No database migration. | Full regional autonomy; smallest blast radius per region. |
| Cons | Requires a schemaless-to-schema migration and new operational capability. | Keeps three hops and the orchestrating process layer. | The complexity the work exists to remove is now permanent. | Cross-region atomicity is unavailable, so compensation stays. |
| Score (out of 5) | 5 | 3 | 2 | 2 |
| Remarks | Chosen. Proven by `A1`, `B1`, `B2`, `B4`; no compensation endpoints by `M`. | Half the benefit for the same migration cost. | Rejected: treats the symptom, not the cause. | Rejected: contradicts the residency-by-policy approach. |

**Engine shortlist**

| | CockroachDB | YugabyteDB | TiDB | Cloud Spanner |
| --- | --- | --- | --- | --- |
| Summary | Distributed SQL, PostgreSQL wire protocol | Distributed SQL, PostgreSQL and Cassandra APIs | Distributed SQL, MySQL compatible | Fully managed distributed SQL |
| Description | Raft per range, strict `SERIALIZABLE` by default, native `REGIONAL BY ROW` placement and per-region zone partitions. | YSQL over a Raft-replicated document layer; geo-partitioning through tablespaces and placement policies. | TiKV plus a stateless SQL layer; placement rules in SQL. | TrueTime-backed external consistency; regions as a deployment concept. |
| Pros | Strictest default isolation; most mature multi-region syntax; self-hostable. | Same guarantees; alternative SQL surface. | Familiar MySQL dialect; strong ecosystem. | Least operational burden. |
| Cons | Operational learning curve; some features are enterprise-only. | Smaller community; weaker geo-partitioning ergonomics. | Default isolation is snapshot / `REPEATABLE READ` and permits write skew. | GCP-only; conflicts with portability. |
| Score (out of 5) | 5 | 4 | 3 | 3 |
| Remarks | Chosen as the reference implementation for the PoC. | Fallback if CockroachDB licensing or operations prove unsuitable. | Rejected for ledger-style invariants. | Rejected on lock-in. |

## Architecture view

```text
Before                                     After
--------                                   -----
experience ---\                            edge tier (thin)
process -------> system APIs ---+          |
                                |          +-- modular monolith (8 modules)
  mongo/uk      mongo/hk        |              party, relationship, contact_point   -> uk
  (separate replica sets)       |              product_holding                        -> hk
  no cross-replica-set txn      |              customer_360 (projection)
  => Saga / TCC / outbox        |          |
                                |          +-- geo-partitioned distributed SQL
                                |              one strict-serializable transaction
                                |              across both regions
```
