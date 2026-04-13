# 002 — Data Integrity — Plan

## Architecture & Stack

- Language: Java 21 (maven `release` 21)
- Build: Maven module `45_IntegrationHub`
- Protobuf: Generated classes live under `io.forest.integrationhub.v1` (compiled by `protobuf-maven-plugin`). Generated classes are excluded from Pitest runs.

## Data Model

- Primary payloads: protobuf RPC messages (e.g., `EchoRequest`, `EchoResponse`). Test vectors live in `specs/002-data-integrity/test-vectors/`.

## Phases

- Phase 1: T002-001, T002-002 — add test vectors and `ValidationService` API; include unit tests.
- Phase 2: T002-003, T002-004 — implement `ChecksumUtils` and an `IdempotencyStore` (default in-memory, pluggable Redis backend).
- Phase 3: T002-005 — broaden tests, CI integration, Pitest/ArchUnit gates.

## Technical Constraints

- Default checksum algorithm: SHA-256; allow override via `integrationhub.checksum.algorithm`.
- Idempotency TTL default: 300 seconds; store interface must support `put(key, value, ttl)` and `get(key)` semantics.
- Pitest: targetClasses for this feature should be `io.forest.integrationhub.integrity.*` with `excludedClasses` set to generated proto packages.

## Implementation Notes

- `ValidationService` signature: `void validateProto(com.google.protobuf.Message message) throws ValidationException`.
- `ChecksumUtils` static helpers: `sha256Hex(String)`, `sha256Hex(byte[])`, `crc32(byte[])`.
- Idempotency: provide `IdempotencyStore` interface and `InMemoryIdempotencyStore` for tests; document Redis implementation as recommended production option.

## Testing & CI

- Unit tests under `45_IntegrationHub/src/test/java/io/forest/integrationhub/integrity`.
- Pitest goals: feature-core mutation score >= 80% (document exceptions); exclude generated protos (`io.forest.integrationhub.v1.*`).
- ArchUnit: no package-dependency violations; include ArchUnit tests already present in repo.

## Task → Requirement Mapping

- T002-001 → FR-002-001 (test vectors)
- T002-002 → FR-002-001 (ValidationService)
- T002-003 → FR-002-002 (ChecksumUtils)
- T002-004 → FR-002-003 (Idempotency)
- T002-005 → FR-002-004 (CI quality)
# 002 — Data Integrity — Plan

## Architecture / Stack

- Use existing Java-based service modules under `45_IntegrationHub`.
- Reuse Protobuf schemas for canonical message formats (specs/010-rpc-middleware already contains examples).
- Add a lightweight validation layer (Jackson/protobuf + explicit validators) and checksum utilities (CRC32 or SHA-256 based on config).

## Data Model

- Message (id, payload, schemaVersion, checksum, metadata)
- StoredRecord (id, payloadRef, checksum, createdAt, processedAt)

## Phases

1. Define schema changes and sample messages (specs and test vectors).
2. Implement validation utilities and schema registry hooks.
3. Implement checksum compute/verify helpers and persistence hooks.
4. Add unit and integration tests for validation, checksum, and idempotency paths.
5. Wire CI (tests + Pitest + ArchUnit) for the new modules.

## Constraints

- Keep the validation and checksum code minimal and fast; avoid blocking main processing threads.
- Prefer well-known checksum algorithms; allow configuration via properties.

## Deliverables

- `specs/002-data-integrity/spec.md`, `plan.md`, `tasks.md` (this feature)
- Implementation stubs under `src/main/java/io/forest/integrationhub/integrity`
- Unit tests under `src/test/java/...` covering invalid input, checksum mismatch, and idempotency.
