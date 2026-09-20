# ADR-002: Application Architecture for the CDM Service

## Problem statement
Which application architecture lets the CDM service commit a composite customer operation as one transaction, without the compensation machinery the current three-layer split requires?

## Background
The platform is split into experience, process and system layers. The process layer orchestrates remote system services and compensates on failure. The PoC folds those layers into a single Spring Modulith deployable of eight modules; the follow-up refactor extracted outbound ports, moved web adapters out of the domain packages, and codified nine layering rules in `ArchitectureTests`.

## Constraints
- A composite CDM operation must commit or abort as one unit, in process.
- Module boundaries must be enforced by the build, not by convention.
- No runtime framework type may leak into a domain package.

## Assumptions
- The system services are thin data-access facades, not independently scaled domains.
- Per-service independent deployment is not required.
- One team owns the CDM domain end to end.

## Options

| | Modular monolith, hexagonal | Layered monolith | Microservices | Clean Architecture, full |
| --- | --- | --- | --- | --- |
| Summary | One deployable; per-context modules exposing ports and adapters | One deployable; code grouped by technical role | One service per domain, over HTTP | One deployable; entities, use cases and gateways per use case |
| Description | Each bounded context is a module whose base package is its API and whose `internal` packages hold the port plus the JDBC and web adapters. | Controllers, services and repositories each form a horizontal layer across all domains. | Party, Relationship, Product Holding etc. become separate processes. | Use cases are explicit classes; each gets its own request/response model and gateway interface. |
| Pros | In-process transactions; boundaries testable; domain isolated from HTTP and SQL; adapters replaceable. | Familiar; simple to start. | Independent scaling and deployment per domain. | Strongest isolation of policy from mechanism. |
| Cons | One runtime and one deploy cycle per deployable. | Re-couples domains by technical layer; erodes the domain boundaries. | Re-introduces the distributed-transaction problem this work removes. | Roughly double the types for the same behaviour. |
| Score (out of 5) | 5 | 2 | 2 | 3 |
| Remarks | Chosen. Enforced by `ModularityTests` (module boundaries) and `ArchitectureTests` (layering). | Would contradict the one-transaction objective. | Rejected: forces Saga/TCC back into the design. | Direction is right; ceremony is not justified at this scale. |

## Architecture view

```text
io.forest.cdm.party                bounded context = one Modulith module
|-- PartyService                  base package = public API (inbound port)
|-- PartyType, PartyRow           domain value objects
|-- PartyNotFoundException        domain failure
`-- internal
    |-- PartyRepository           outbound port (interface)
    |-- JdbcPartyRepository       driven adapter   ---> CockroachDB
    `-- web/PartyController       driving adapter  <--- HTTP

Dependency direction, enforced by ArchUnit:
    web ---> public API ---> outbound port <--- adapter
    (no HTTP types below web)   (no SQL types above the port)
```
