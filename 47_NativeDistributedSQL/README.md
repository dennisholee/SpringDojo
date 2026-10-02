# CDM Platform PoC - Native Distributed SQL + Modular Monolith

A working Proof of Concept for the bank's Customer Data Management (CDM) platform. It demonstrates
that a **geo-partitioned Distributed SQL database** removes the need for Saga / TCC / outbox
compensation, and that collapsing the experience / process / system layers into a **modular
monolith** turns cross-domain orchestration into a single in-process SQL transaction.

See [`architecture.md`](architecture.md) for the specification and
[`docs/adr/`](docs/adr) for the decision records, ADR-001 to ADR-006.

## What is being proven

| Pattern | Description | Endpoint |
| --- | --- | --- |
| **A - region-local ACID** | Party, Relationship and Contact Points are all UK-mastered, so the transaction never leaves the `uk` region. | `POST /api/onboarding/local` |
| **B - cross-region ACID** | The same transaction also creates an HK product holding. The engine coordinates both regions; there is no compensation handler. | `POST /api/onboarding/cross-region` |
| **B (negative)** | A failure after the cross-region writes rolls the whole transaction back. | `POST /api/onboarding/cross-region/rollback-demo` |
| **Residency** | Rows never leave their home region. | `GET /api/residency` |

## Prerequisites

- Java 21+ and Maven 3.9+
- Docker with the Compose plugin
- RAM: ~6 GB for the canonical six-node cluster, or ~2 GB for the lite four-node cluster

### Two-region topologies available

| File | Nodes | Use it for |
| --- | --- | --- |
| `docker/docker-compose.yml` | 6 (3 UK + 3 HK) | Full Pattern A / B comparison and any latency measurement |
| `docker/docker-compose.lite.yml` | 4 (2 UK + 2 HK) | A functional smoke test on a constrained machine; ranges may be under-replicated, so do not measure latency on it |

Both use the same host ports, so run one at a time.

## 1. Start the two-region cluster

Canonical six-node topology:

```bash
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml logs -f init
```

Lite four-node topology (smaller machines):

```bash
docker compose -f docker/docker-compose.lite.yml up -d
docker compose -f docker/docker-compose.lite.yml logs -f init
```

`init` waits for the nodes, initializes the cluster, applies `docker/init-db.sql` and prints the
resulting region placement. You want it to finish with `[init] done.`

Admin UIs: <http://localhost:8080> (UK) and <http://localhost:8081> (HK).

## 2. Run the application

```bash
mvn spring-boot:run
```

The app connects to `jdbc:postgresql://localhost:26257/cdm` (the UK node). Point `CDM_DB_URL` at the
HK node (`jdbc:postgresql://localhost:26258/cdm?sslmode=disable`) to see how latency behaves when
the same call is issued from the other region.

## 3. Exercise the scenarios

Pattern A - region-local, UK only:

```bash
curl -s -X POST http://localhost:8080/api/onboarding/local \
  -H 'Content-Type: application/json' \
  -d '{
        "legalName": "Tan Wei Ltd",
        "partyType": "ORGANIZATION",
        "market": "UK_RETAIL",
        "lineOfBusiness": "RETAIL",
        "contactPointType": "EMAIL",
        "contactPointValue": "ops@tanwei.example",
        "productType": "CURRENT_ACCOUNT",
        "accountNumber": "UK-0001"
      }'
```

Pattern B - cross-region, UK core domains plus an HK product holding:

```bash
curl -s -X POST http://localhost:8080/api/onboarding/cross-region \
  -H 'Content-Type: application/json' \
  -d '{
        "legalName": "Tan Wei Ltd",
        "partyType": "ORGANIZATION",
        "market": "HK_INSURANCE",
        "lineOfBusiness": "INSURANCE",
        "contactPointType": "PHONE",
        "contactPointValue": "+852 1234 5678",
        "productType": "SAVINGS_ACCOUNT",
        "accountNumber": "HK-0001"
      }'
```

Pattern B negative test - expect `"rolledBack": true` and identical `before` / `after` snapshots:

```bash
curl -s -X POST http://localhost:8080/api/onboarding/cross-region/rollback-demo \
  -H 'Content-Type: application/json' \
  -d '{
        "legalName": "Rollback Ltd",
        "partyType": "ORGANIZATION",
        "market": "HK_INSURANCE",
        "lineOfBusiness": "INSURANCE",
        "contactPointType": "EMAIL",
        "contactPointValue": "no@orphan.example",
        "productType": "SAVINGS_ACCOUNT",
        "accountNumber": "HK-0002"
      }'
```

Residency proof - `party`, `relationship` and `contactPoint` must only appear under `uk`, and
`productHolding` only under `hk`:

```bash
curl -s http://localhost:8080/api/residency
```

## 4. Run the tests

### 4a. Unit and architecture tests (no Docker)

```bash
mvn test
```

`ModularityTests.moduleBoundariesAreRespected` fails the build if any module reaches into another
module's `internal` package or a dependency cycle appears. This test needs no database.

### 4b. End-to-end suite (no mocks, requires Docker)

```bash
mvn verify -Pe2e
```

This boots a real two-region CockroachDB cluster with `docker compose`, starts the application against
it, drives every proof of concept over real HTTP and writes one report per PoC.

| PoC | What it proves |
| --- | --- |
| A1 | UK-only onboarding is one region-local transaction |
| A2 | HK-local product holding, and rejection of an unknown party |
| B1 | UK + HK onboarding in one transaction, with timings recorded |
| B2 | Simulated **and real** failures roll the UK writes back |
| B3 | Concurrent cross-region writers are serialised (exactly one commits) |
| B4 | Losing Hong Kong's quorum aborts the write atomically, then the region recovers |
| R1 | Logical and physical replica placement stays in-region |
| M | No compensation endpoints, no saga/outbox tables |
| J | The 360 view is complete and consistently homed |
| MIG | MongoDB to Distributed SQL migration closes the legacy gap |
| RM | The 360 read model is decoupled from the write and converges |

Artefacts:

- `target/cucumber-reports/cucumber.html` - readable end-to-end report
- `target/site/jacoco/index.html` - code coverage
- `target/poc-reports/poc-coverage-matrix.md` - one row per PoC
- `target/poc-reports/poc-<id>.md` - scenarios and module coverage for that PoC

There are no `@MockBean`, no stubs and no in-memory database anywhere in the suite, and it uses the
same `docker/init-db.sql` as the manual path so the schema cannot drift.

The default topology is a **composed** one: the canonical six-node CockroachDB cluster plus the
legacy MongoDB replica set. `cdm.e2e.compose` takes a comma-separated list, so a lighter
CockroachDB-only run is still possible:

```bash
# default: 6 CockroachDB nodes + MongoDB (needed by B4, MIG and RM)
mvn verify -Pe2e

# lighter, CockroachDB only (B4, MIG and RM will fail without MongoDB / 3 nodes per region)
mvn verify -Pe2e -Dcdm.e2e.compose=docker/docker-compose.lite.yml
```

To iterate quickly, start the topology once and reuse it:

```bash
docker compose -p cdm-e2e \
  -f docker/docker-compose.yml -f docker/docker-compose.mongo.yml up -d
mvn verify -Pe2e -Dcdm.e2e.reuse=true
docker compose -p cdm-e2e \
  -f docker/docker-compose.yml -f docker/docker-compose.mongo.yml down -v
```

## 5. Tear down

```bash
docker compose -f docker/docker-compose.yml down -v
# or, for the lite topology
docker compose -f docker/docker-compose.lite.yml down -v
```

## Verified in this repository

- `mvn verify -Pe2e` ran the original 13-scenario suite (A1, A2, B1-B4, R1, M, J) against a real
  two-region CockroachDB cluster: **15 tests, 0 failures**, with **92.7% - 100% line coverage** per
  proof of concept (`target/poc-reports/poc-coverage-matrix.md`).
- `mvn verify` (no Docker) passes: **20 tests** — `ModularityTests` (2, module boundaries),
  `ArchitectureTests` (9 hexagonal-layering, DTO-naming and CQRS rules), `PartyServiceTests` (5) and
  `ProductHoldingServiceTests` (4), the latter two driving the services through hand-written fake
  ports with no mocking framework and no Spring context.
- 99 Gherkin steps bind to 60 step definitions.
- `docker compose config` parses the CockroachDB, lite and MongoDB topologies.

### Implemented but not yet executed

The two Phase 2 proofs of concept (`@PoC-MIG`, `@PoC-RM`) are implemented, compile, and are fully
wired into the features, the composed topology and the reporting. **Their scenarios have not been
run**: this machine's Docker daemon failed mid-session (disk exhaustion) and could not be restarted.
Treat their results as unproven until `mvn verify -Pe2e` has been run on a working Docker host.

## Findings from the first real run

- The `SURVIVAL GOAL` statement in the original schema was invalid syntax; `ZONE` is already the
  default for this topology, so it was removed.
- **At least three nodes per region are required** for in-region replica placement. The four-node
  lite topology cannot keep a replication factor of three inside a region, so replicas and
  leaseholders spill across the boundary. Use the six-node topology for residency or latency work.
- Range-level replica localities are **not** a valid residency check for `REGIONAL BY ROW` tables
  (empty foreign-region ranges always exist). Residency is asserted on the row's `crdb_region` plus
  the per-region `voter_constraints`.

## Running the end-to-end suite

```bash
docker pull cockroachdb/cockroach:latest-v23.2   # one-off
mvn verify -Pe2e
```

To iterate quickly, start the cluster once and reuse it:

```bash
docker compose -p cdm-e2e6 -f docker/docker-compose.yml up -d
mvn verify -Pe2e -Dcdm.e2e.reuse=true
docker compose -p cdm-e2e6 -f docker/docker-compose.yml down -v
```
