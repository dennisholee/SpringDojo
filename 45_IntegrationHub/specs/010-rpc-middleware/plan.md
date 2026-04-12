# Implementation Plan — RPC Middleware (gRPC)

Assumed stack: Java 21, Maven, gRPC / Protobuf, JUnit 5. If your stack differs, tell me and I'll adapt the plan.

## Goal
Deliver a production-ready RPC middleware module that provides Protobuf contracts, generated stubs, adapter-layer mappings, deterministic integration tests, and a CI workflow that runs fast PR smoke checks and nightly performance benchmarks.

## Phases & Tasks

Phase 0 — Setup (0.5 day)
- Confirm tech stack and developer environment (JDK 21, Maven). Output: this plan updated.
- Create repository/module skeleton and directories: `protos/`, `src/main/java/io/forest/integrationhub/adapters/rpc`, `src/test`.

Phase 1 — Design (1–2 days)
- Draft Protobuf contracts (messages and services) covering primary RPCs.
- Document transport-to-domain mapping rules in `specs/010-rpc-middleware/spec.md` (adapters perform mapping; core stays mapping-free).
- Define schema-evolution policy and versioning approach (backwards-compatible; bump package for breaking changes).

Phase 2 — Implementation (2–4 days)
- Add `protobuf-maven-plugin` configuration to `pom.xml` for stub generation.
- Generate server and client stubs during the build using `protobuf-maven-plugin`; DO NOT commit generated files to VCS. CI builds must generate stubs as part of the pipeline and use them for tests.
- Implement adapter mapping classes with unit tests for mapping correctness.

Phase 3 — Integration & Tests (1–3 days)
- Implement integration tests exercising generated stubs and verifying contract compatibility.
- Add smoke tests that run quickly in PRs and a fuller integration suite for CI `verify` phase.

Phase 4 — Performance & CI (1–2 days)
- Add a nightly GitHub Action to run the performance harness (full benchmarks).
- Add PR workflow that runs `mvn -Dskip.performance=true verify` or similar for fast feedback (unit + integration smoke).
- Define the performance benchmark harness and thresholds: median latency <50ms under representative integration load (nightly); PRs run smoke tests only.
- Add CI enforcement steps for ArchUnit fitness-function tests and Pitest mutation testing. Configure Pitest thresholds per the constitution (core >=90%, adapters >=80%) and run mutation checks in CI; consider nightly extended mutation analysis if needed.

Phase 5 — Docs & Quickstart (0.5 day)
- Add `quickstart.md` with commands to generate stubs, run unit/integration tests, and execute benchmark smoke runs.

Phase 6 — Release & Review (0.5 day)
- Open a feature branch, push changes, and request reviewers. Include a clear summary of mapping rules and performance targets in the PR description.

## Deliverables
- `specs/010-rpc-middleware/plan.md` (this file)
- `specs/010-rpc-middleware/protos/*.proto` (contract drafts)
- `pom.xml` plugin updates for `protobuf-maven-plugin`
- Adapter implementation: `io.forest.integrationhub.adapters.rpc.*`
- Tests: unit mapping tests, integration contract tests, smoke tests
- CI workflows: `.github/workflows/rpc-verify.yml` and `.github/workflows/rpc-nightly-benchmarks.yml`
- `quickstart.md`

## Estimates (rough)
- Total: 6–11 working days depending on review cycles and existing infra for benchmarking.

## Acceptance Criteria
- All unit tests and PR smoke tests pass in CI.
- Integration tests validate contract compatibility between client and server stubs.
- Performance nightly job completes and reports median latency; PRs do not block on full benchmarks.
- Documentation and quickstart reproduce the local verification steps.

## Risks & Mitigations
- Risk: Benchmark environment variability → Mitigation: use representative controlled environment (nightly) and treat PR runs as smoke checks only.
- Risk: Breaking proto changes → Mitigation: versioned proto packages and policy documented in spec.

## Next Actions (immediate)
1. Confirm tech stack (Java 21, Maven, gRPC/Protobuf) — if confirmed, I'll mark task done and initialize module skeleton.
2. Draft the first `.proto` with the most common RPCs.
3. Add `protobuf-maven-plugin` configuration and generate stubs locally.
