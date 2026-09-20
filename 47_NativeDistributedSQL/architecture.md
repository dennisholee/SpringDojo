# Architectural Specification: Native Distributed SQL Transaction PoC

## 1. Objective & Pattern Shift
The goal of this Proof of Concept (PoC) is to demonstrate how a Distributed SQL database completely eliminates the need for application-level distributed transaction patterns such as Try-Confirm-Cancel (TCC) or Saga. 

The domains that were previously split across microservices are collapsed into a single **modular monolith** (see section 7), whose modules share one Distributed SQL storage layer. Distributed ACID transactions must be handled natively by the database engine via consensus protocols (Raft/Paxos), not by the application. The application layer must completely avoid writing compensation logic, fallback states, or outbox retry daemons.

## 2. Core Constraints & Guarantees
- **No TCC Logic:** Services must NOT expose `/try`, `/confirm`, or `/cancel` endpoints. They will expose standard REST or gRPC mutations.
- **Database Engine:** CockroachDB (reference implementation; YugabyteDB is the evaluated alternative), running a local **two-region, six-node** cluster - three nodes in `uk`, three in `hk`. See `docker/docker-compose.yml`.
- **Isolation Level:** STRICT SERIALIZABLE isolation across all operations.
- **No Application Compensation:** Data rollbacks must be handled exclusively by executing standard SQL `ROLLBACK` commands or relying on automatic cluster-wide transaction aborts.

## 3. Technology Stack (decided for this PoC)

| Concern | Choice | Rationale |
| --- | --- | --- |
| Database | **CockroachDB**, 2 regions x 3 nodes | Strict `SERIALIZABLE` by default and mature `REGIONAL BY ROW` geo-partitioning. YugabyteDB is the evaluated fallback; TiDB is rejected for this workload because its default isolation is snapshot / `REPEATABLE READ` and can permit write skew. |
| Application | **Java 21 / Spring Boot 3.5 / Spring Modulith 1.4** | In-process module calls replace cross-service HTTP; `ApplicationModules.verify()` fails the build on boundary violations. |
| Persistence API | Spring JDBC over the PostgreSQL wire protocol | No vendor driver lock-in, and explicit SQL keeps region placement visible in the code. |
| Orchestration | **None** | The process-layer orchestrator is deleted. A single `@Transactional` call spans the modules; the engine owns atomicity. |

## 4. Consistency Patterns under Test

Not every operation needs the same guarantee. The PoC distinguishes three patterns and proves that
the choice is a **database** concern, not an application concern.

| Pattern | Consistency | Residency | Latency | Typical CDM use |
| --- | --- | --- | --- | --- |
| **A - region-local ACID** | Strong within a region | Preserved by geo-partitioning | Local | Maintain Party / Relationship / Contact Points (UK) or Product Holding (HK) |
| **B - cross-region ACID** | Strong globally, strict serializable | Preserved (data stays partitioned; only coordination crosses) | Elevated, and measured | Onboarding that must atomically create a UK party and an HK product holding |
| **C - write-local / read-global** | Eventual across regions | Preserved | Local writes, async reads | Customer-360 view spanning UK and HK |

The design principle: **pay for cross-region ACID only where atomicity is genuinely required.**
Customer-360 is a *read* requirement, so it belongs to Pattern C (an eventually consistent read
model fed by CDC) rather than to Pattern B.

## 5. Pattern A vs Pattern B - the PoC Matrix

Both patterns are exercised side by side against the same cluster, the same schema and the same
application code. The only difference is which regions the touched rows live in.

| # | Operation | Tables touched | Regions | Pattern | What it proves |
| --- | --- | --- | --- | --- | --- |
| A1 | Onboard a UK-only customer | `party`, `relationship`, `contact_point` | `uk` | A | Single in-process SQL transaction; no compensation code; local commit latency |
| A2 | Create / update a product holding | `product_holding` | `hk` | A | Local latency; isolation from the UK write path |
| B1 | Onboard an HK customer as a new party | all four tables | `uk` + `hk` | B | Atomicity across regions in one transaction |
| B2 | Failed cross-region onboarding | all four tables | `uk` + `hk` | B | Mid-flight failure aborts the whole transaction - no orphaned party |
| B3 | Two concurrent onboardings for one account | all four tables | `uk` + `hk` | B | Exactly one writer commits; the loser rolls its party write back |
| B4 | HK loses quorum mid-onboarding | all four tables | `uk` + `hk` | B | The commit fails and no orphaned UK party survives; recovery follows |
| MIG | Legacy MongoDB to CDM migration | CDM tables + MongoDB | `uk` + `hk` | - | Backfill, dual-write, change-stream reconciliation and idempotency converge the gap to zero |
| RM | Eventually consistent 360 read model | `customer_360` projection | `uk` + `hk` | C | The write path leaves the projection stale; a projector converges it |
| R1 | Residency audit | all four tables | - | A + B | UK rows never appear in `hk`, and vice versa |

Exposed by `OnboardingController`:

| Endpoint | Pattern |
| --- | --- |
| `POST /api/onboarding/local` | A (UK-local) |
| `POST /api/product-holdings` | A (HK-local) |
| `POST /api/onboarding/cross-region` | B |
| `POST /api/onboarding/cross-region/rollback-demo` | B (negative test) |
| `GET /api/residency` | A + B (residency proof) |

## 6. Data Residency

Residency is non-negotiable for UK and HK customer data, and geo-partitioning is what preserves it:

- `party`, `relationship`, `contact_point` are `REGIONAL BY ROW` and always written with
  `crdb_region = 'uk'`.
- `product_holding` is `REGIONAL BY ROW` and always written with `crdb_region = 'hk'`.

With the `ZONE` survival goal, every row's replicas are held inside the row's own region, so a UK
row is never stored on an HK node. `GET /api/residency` is the audit that proves it.

**Finding that affects the target programme design:** with only two regions (`uk`, `hk`)
CockroachDB **cannot** set `SURVIVAL GOAL = REGION`. Surviving the loss of a whole region requires a
Raft quorum that can live outside it, which needs at least three regions. The options are therefore:

1. accept `ZONE` survival (a multi-zone failure inside a region can lose data), or
2. add a third region purely as a replica holder (a residency-safe "witness" region holding no
   customer data of its own), or
3. run two independent single-region clusters and accept Pattern C between them.

This is recorded in `docs/adr/ADR-004-cross-region-consistency-and-residency.md`.

**Topology requirement (measured, not assumed):** in-region replica placement needs **at least three
zones (nodes) per region**. The four-node lite topology (two per region) cannot satisfy a replication
factor of three inside a region, so replicas *and* leaseholders spill across the region boundary even
though every row's `crdb_region` is correct. Use the canonical six-node topology for any residency or
latency measurement; treat the lite topology as functional-only.

## 7. Application Architecture - from 3 layers to a modular monolith

The original platform was split into experience, process and system layers, with the process layer
acting as the transaction orchestrator. A composite "onboard a customer" operation therefore cost
several network hops plus compensation logic.

The PoC replaces that with a single deployable composed of four domain modules plus one use-case
module. Boundaries are enforced by the compiler and by `ModularityTests`, not by HTTP.

| Before | After |
| --- | --- |
| experience layer (BFF) | retained as an edge concern; can remain a thin gateway |
| process layer (orchestrator) | folded into the `onboarding` module as an in-process facade |
| system services (Party, Relationship, Contact Points, Product Holding) | `party`, `relationship`, `contactpoint`, `productholding` modules |
| Saga / TCC / Outbox compensation | deleted; one SQL transaction and a database-driven `ROLLBACK` |
| separate Mongo replica sets per region | one geo-partitioned distributed SQL database |

## 8. Repository Layout

Each bounded context is one Spring Modulith module. Its base package is the module's public API
(application service, domain value objects, exceptions); the nested `internal` packages hold the
ports and the adapters, so they are invisible to every other module.

```text
.
|-- architecture.md                  this document
|-- README.md                        how to run the PoC
|-- docker/
|   |-- docker-compose.yml           canonical 2-region CockroachDB cluster (6 nodes)
|   |-- docker-compose.lite.yml      smaller 4-node cluster for functional work
|   |-- docker-compose.mongo.yml     legacy MongoDB replica set (migration PoC)
|   `-- init-db.sql                  multi-region database, REGIONAL BY ROW tables
|-- docs/adr/                        ADR-001 to ADR-006
|-- pom.xml
|-- scripts/generate_poc_report.py   one report per proof of concept
`-- src/
    |-- main/java/io/forest/cdm/
    |   |-- shared/                  shared kernel: Region, CdmDomainException
    |   |-- party/                   PartyService, PartyType, PartyRow, PartyNotFoundException
    |   |   `-- internal/            PartyRepository (port) + JdbcPartyRepository (adapter)
    |   |-- relationship/            same shape
    |   |-- contactpoint/            same shape
    |   |-- productholding/          same shape + internal/web/ProductHoldingController
    |   |-- onboarding/              application layer: OnboardingService
    |   |   `-- internal/web/        OnboardingController, GlobalExceptionHandler
    |   |-- migration/               MigrationService + LegacyMongoRepository, MigrationWriter
    |   |   `-- internal/web/        MigrationController
    |   `-- readmodel/               query side: ReadModelService, Customer360View
    |       `-- internal/            ProjectionRepository (port) + JdbcProjectionRepository + web/
    `-- test/java/io/forest/cdm/
        |-- ModularityTests.java     Spring Modulith module boundaries
        |-- ArchitectureTests.java   hexagonal layering + CQRS rules (ArchUnit)
        `-- e2e/                     Cucumber suite (real cluster, no mocks)
```

### 8a. Layering Rules (enforced by ArchitectureTests)

| Rule | Why it matters |
| --- | --- |
| Spring MVC / HTTP types may only appear under `internal.web` | keeps web concerns out of services and domain types |
| Only web adapters may depend on `internal.web` | pins the dependency direction: application types must never depend on HTTP DTOs |
| Types named `*Request` / `*Result` must live under `internal.web` | keeps DTO naming a reliable layer marker |
| Every `*NotFoundException` must extend `CdmDomainException` | one base type, therefore one HTTP translation point |
| The domain side must not depend on `Jdbc*` classes | forces all persistence access through the port interface |
| Domain types must not depend on `java.sql`, Spring JDBC or the Mongo driver | isolates the domain from persistence technology |
| The command side must not depend on `readmodel` | keeps the CQRS read model out of the write transaction |
| `shared` must not depend on any bounded context | keeps the shared kernel a leaf |

## 9. PoC Deliverables & Success Criteria

1. **Pattern A** completes as a single SQL transaction with no compensation code and no
   cross-region traffic on the commit path.
2. **Pattern B** is atomic: `POST /api/onboarding/cross-region/rollback-demo` reports
   `"rolledBack": true`, and no orphaned `party` row survives.
3. **Residency** holds at all times: `GET /api/residency` shows zero rows outside each domain's
   home region.
4. **Boundaries** hold: `mvn test` passes `ModularityTests.moduleBoundariesAreRespected`.
5. The application contains **no** `/try`, `/confirm` or `/cancel` endpoints, and no outbox table.

## 9a. Automated End-to-End Verification

Every criterion above is asserted by a Cucumber scenario that drives the running application over
real HTTP and then reads the outcome back from the real cluster. There are no mocks, no stubs and no
in-memory database anywhere in the suite.

Run it with:

```bash
mvn verify -Pe2e
```

Each feature is tagged with the proof of concept it belongs to, which yields one report per PoC:

| Tag | Feature file | Proves |
| --- | --- | --- |
| `@PoC-A1` | `a1_uk_local_onboarding.feature` | UK-only onboarding is one region-local transaction |
| `@PoC-A2` | `a2_hk_product_holding.feature` | HK-local product holding, plus rejection of an unknown party |
| `@PoC-B1` | `b1_cross_region_onboarding.feature` | UK + HK onboarding in one transaction; timings recorded |
| `@PoC-B2` | `b2_cross_region_rollback.feature` | Simulated and real failures both roll the UK writes back |
| `@PoC-B3` | `b3_concurrent_onboarding.feature` | Concurrent cross-region writers are serialised |
| `@PoC-B4` | `b4_region_quorum_loss.feature` | Losing a region's quorum aborts the write atomically, then recovers |
| `@PoC-R1` | `r1_residency.feature` | Logical **and physical** replica placement stays in-region |
| `@PoC-MIG` | `migration_mongodb_to_distributed_sql.feature` | Backfill, dual-write and reconciliation close the legacy gap |
| `@PoC-RM` | `readmodel_customer_360.feature` | The 360 projection is decoupled from the write and converges |
| `@PoC-M` | `m_no_saga.feature` | No compensation endpoints and no saga/outbox tables |
| `@PoC-J` | `j_full_journey.feature` | The 360 view is complete and consistently homed |

Artefacts: `target/cucumber-reports/cucumber.html`, `target/site/jacoco/` and
`target/poc-reports/poc-coverage-matrix.md` plus one `poc-<id>.md` per proof of concept.

Residency is checked twice: logically through the `crdb_region` column on every row, and physically
through the per-region zone configuration, which must pin that partition's **voting replicas** to the
home region (`voter_constraints = '[+region=...]'`).

Raw range localities are deliberately **not** used as a residency check: a `REGIONAL BY ROW` table's
key space is pre-split for every region, so empty foreign-region ranges always appear and would
produce a false reading.

## 10. Explicitly Out of Scope

- Performance tuning beyond the comparative latency of Pattern A and Pattern B.
- Production concerns: TLS, authn/authz, backups, DR runbooks, multi-tenancy.

## 11. Evidence and Open Gaps

Section 9 states what the PoC set out to prove. This section states plainly which parts are actually
**proven by running code**, and which are not. It is the honest counterpart to section 9.

| Hypothesis claim (from section 1) | End-to-end evidence | Status |
| --- | --- | --- |
| No Saga / TCC / outbox compensation machinery | `M`: `POST /try`, `/confirm`, `/cancel` return 404; no `saga` or `outbox` table exists | **Proven** |
| A composite cross-domain operation is one transaction | `A1` (three UK tables) and `B1` (four tables across two regions) each commit in one request | **Proven** |
| Rollback is the database's job, not the application's | `B2`: a simulated failure **and** a real unique-constraint violation both roll the UK writes back | **Proven** |
| Cross-region writes stay atomic when a region fails | `B4`: with two of the three HK nodes stopped the commit fails (`HTTP 500`) and leaves **no orphaned UK party**; reads and writes recover afterwards | **Proven** |
| Concurrent writers cannot both commit | `B3`: two concurrent onboardings for one account number - exactly one commits, the loser rolls its party write back | **Proven** |
| Residency is preserved by geo-partitioning | `R1`: every row's `crdb_region` is correct, and the per-region `voter_constraints` pin voting replicas to the home region | **Proven** |
| The engine coordinates ACID via consensus (Raft) | observed only as an *effect* (atomic cross-region commit and abort), never as a mechanism | **Implied, not directly observable** |
| Collapsing three layers into a monolith improves latency | no three-layer baseline was built, so there is nothing to compare against | **Not proven** |
| A cross-region commit costs more than a region-local one | measured on one host as ~16-24 ms (`A`) vs ~18-45 ms (`B`) - overlapping, therefore not meaningful. The assertion exists but is gated behind `-Dcdm.e2e.assertLatency=true` for genuinely separated regions | **Not proven here** |
| MongoDB to Distributed SQL migration converges to a zero gap | `MIG`: backfill, dual-write, change-stream reconciliation and idempotency | **Implemented, pending a Docker run** |
| A customer-360 read model can be eventually consistent | `RM`: the write path leaves the projection stale and the projector converges it | **Implemented, pending a Docker run** |

### What this means

The suite proves the **correctness half** of the hypothesis: cross-domain and cross-region writes are
atomic, failure needs no compensation code, concurrent writers are serialised, and residency holds
under all of it. It does **not** prove the **performance half**, which is the stated motivation for
the change, because no three-layer baseline exists and every node runs on one host here.

The two Phase 2 proofs of concept are now implemented (`@PoC-MIG`, `@PoC-RM`) but have not been
executed, so their rows above read "pending a Docker run" rather than "proven".

### Honest limitations

- **Single-host topology.** All six nodes share one kernel, so inter-region network latency is not
  realistic. The A/B timings are recorded as evidence, not asserted as a budget.
- **Only one failure mode is injected.** Quorum loss is covered; node crash during the commit retry
  window, clock skew and asymmetric network partition are not.
- **`J` bypasses a read API.** It proves durable consistency of the write, not a read-model contract.
- **Coverage is 92.7-100% line, not 100%.** The residual branches are unexecuted fault paths; they are
  enumerated in `target/poc-reports/` rather than papered over.

