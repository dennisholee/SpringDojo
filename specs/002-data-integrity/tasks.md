# 002 — Data Integrity — Tasks

## Tasks

T002-001 — Define canonical message examples and test vectors
- Place example messages in `specs/002-data-integrity/test-vectors/`.

T002-002 — Implement validation utilities
- Add `io.forest.integrationhub.integrity.ValidationService` with schema validation hooks.

T002-003 — Implement checksum helpers
- Add `io.forest.integrationhub.integrity.ChecksumUtils` (configurable algorithm).

T002-004 — Add idempotency support
- Design and implement duplicate detection (idempotency keys) with unit tests.

T002-005 — Tests and CI integration
- Add unit and integration tests under `src/test/java` and ensure Pitest and ArchUnit are included in CI.

## Phases

- Phase 1: T002-001, T002-002
- Phase 2: T002-003, T002-004
- Phase 3: T002-005
