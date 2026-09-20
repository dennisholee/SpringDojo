# ADR-004: Cross-Region Consistency and Data Residency

## Problem statement
Which consistency guarantee should each CDM operation get, given that UK and HK customer data must not leave its region?

## Background
Core domains (Party, Relationship, Contact Point) are mastered in the UK; Product Holding is mastered in the HK market. One customer onboarding touches both, so residency pulls the data apart while consistency pulls it together. The PoC measured both directions: region-local writes (`A1`, `A2`), cross-region ACID (`B1`), rollback (`B2`), concurrent writers (`B3`), region-quorum loss (`B4`), residency (`R1`), and an eventually consistent 360 view (`RM`).

## Constraints
- Data residency is non-negotiable: UK rows on UK nodes, HK rows on HK nodes.
- Isolation is strict serializable inside the database.
- A cross-region commit costs at least one extra network round trip and must be budgeted, not wished away.

## Assumptions
- Customer-360 is a read requirement, not a write requirement.
- Operations that genuinely need cross-region atomicity are a small minority.
- A bounded staleness window is acceptable to consumers of the 360 view.
- A third region, if required for region survival, would hold replicas but no customer data.

## Options

| | A + B + C | A only | B everywhere | Two independent clusters |
| --- | --- | --- | --- | --- |
| Summary | Home-region master; cross-region ACID only where atomicity is real; eventual 360 | Region-local writes and reads only, with async replication between regions | Every write is a cross-region ACID transaction | One database per region, reconciled in the application |
| Description | Each row is homed in its master region (`REGIONAL BY ROW`). Operations confined to one region commit locally (A). Onboarding that must create a UK party and an HK holding uses one cross-region transaction (B). The 360 view is a projection fed asynchronously (C). | All writes are region-local; a foreign region sees the other region's data only after replication. | Residency still holds per row, but every commit coordinates both regions. | Two separate clusters, no shared transaction. |
| Pros | Local latency where possible; native atomicity where required; residency by policy. | Cheapest possible writes; simple. | Uniform behaviour; no per-operation decision. | Full regional autonomy. |
| Cons | Two patterns to reason about, and a staleness window on the view. | The 360 view has no consistent answer; cross-region workflows lose atomicity. | Every write pays the cross-region penalty, including the majority that need not. | No atomic cross-region operation at all; reconciliation becomes the compensation this work removes. |
| Score (out of 5) | 5 | 3 | 2 | 2 |
| Remarks | Chosen. Proven by `A1`/`B1`/`B2`/`B4`; residency by `R1`; the view by `RM`. | Acceptable only if every cross-region workflow can tolerate eventual consistency. | Rejected on latency grounds given the onboarding path. | Rejected: reintroduces application compensation. |

## Architecture view

```text
          region uk                                region hk
   +--------------------------+             +--------------------------+
   | party                    |   A local   | product_holding          |
   | relationship             |<----------->| (market data)            |
   | contact_point            |             | crdb_region = 'hk'       |
   | crdb_region = 'uk'       |             |                          |
   +--------------------------+             +--------------------------+
                |                                        |
                |  B  one strict-serializable transaction |
                +------------ ( onboarding ) ------------+
                                  |
                C  CDC ---> customer_360 projection (eventually consistent)

Measured, not assumed:
  - in-region replica placement needs at least three nodes (zones) per region
  - a two-region database cannot set SURVIVAL GOAL = REGION
  - residency is asserted on each row's crdb_region plus the per-region
    voter_constraints; range localities are not a valid check for
    REGIONAL BY ROW tables
```
