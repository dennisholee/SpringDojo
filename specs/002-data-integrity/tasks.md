# 002 — Data Integrity — Tasks

## Tasks

T002-001 — Define canonical message examples and test vectors
- Place example messages in `specs/002-data-integrity/test-vectors/`.
 - Related FR: FR-002-001
 - Acceptance: At least 3 canonical test vectors (valid/invalid/edge-case) saved under `specs/002-data-integrity/test-vectors/` and consumed by unit tests.

T002-002 — Implement validation utilities
- Add `io.forest.integrationhub.integrity.ValidationService` with schema validation hooks.
 - Related FR: FR-002-001
 - Acceptance: `ValidationService.validateProto(Message)` validates `EchoRequest` examples and throws `ValidationException` on invalid inputs; unit tests added.

T002-003 — Implement checksum helpers
- Add `io.forest.integrationhub.integrity.ChecksumUtils` (configurable algorithm).
 - Related FR: FR-002-002
 - Acceptance: `sha256Hex("hello")` matches known vector; `crc32` matches Java CRC32; algorithm overridable via `integrationhub.checksum.algorithm`.

T002-004 — Add idempotency support
- Design and implement duplicate detection (idempotency keys) with unit tests.
 - Related FR: FR-002-003
 - Acceptance: Provide `IdempotencyStore` interface and an `InMemoryIdempotencyStore` used in tests; TTL behavior covered by unit tests. Define canonical behavior: duplicate requests within TTL MUST return cached result with HTTP 200 and header `X-Idempotency-Status: replay`. Tests MUST assert header presence and TTL expiry semantics.

T002-006 — Add complexity and PMD checks
- Add PMD/Checkstyle rules and a CI task to enforce cyclomatic complexity ceilings (core methods <= 6). Fail the build if exceeded for modified core methods.
 - Related FR: FR-002-004
 - Acceptance: PMD/Checkstyle integrated and CI job reports complexity violations as build-blocking.

T002-005 — Tests and CI integration
- Add unit and integration tests under `src/test/java` and ensure Pitest and ArchUnit are included in CI.
 - Related FR: FR-002-004
 - Acceptance: CI executes unit tests, ArchUnit checks, and Pitest with `io.forest.integrationhub.integrity.*` targeted; feature-core mutation score >= 80% (or documented exception in PR).

## Phases

- Phase 1: T002-001, T002-002
- Phase 2: T002-003, T002-004
- Phase 3: T002-005
