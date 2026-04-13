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
