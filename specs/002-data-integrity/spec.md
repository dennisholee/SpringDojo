# 002 — Data Integrity — Specification

## Overview

Ensure message-level data integrity and validation for protobuf RPC messages handled by Integration Hub. Provide deterministic checksum utilities, schema/field validation, and a pragmatic idempotency mechanism. Deliver test vectors and measurable quality gates for CI.

## Functional Requirements

- FR-002-001: Message validation
  - The system MUST validate incoming protobuf messages against expected constraints (required fields, non-empty payloads, field-level rules) and reject invalid messages with a `ValidationException`.
  - Acceptance: `ValidationService.validateProto(Message)` throws `ValidationException` for invalid `EchoRequest` examples; unit tests present under `src/test/java/io/forest/integrationhub/integrity`.

- FR-002-002: Checksum utilities
  - Provide `ChecksumUtils` implementing SHA-256 hex and CRC32 outputs. Default algorithm: SHA-256; algorithm configurable via `integrationhub.checksum.algorithm` system property.
  - Acceptance: Known-test vectors in `specs/002-data-integrity/test-vectors/` validate outputs; unit tests verify `sha256Hex("hello")` and `crc32(bytes)`.

- FR-002-003: Idempotency support
  - Provide an idempotency detection API and an in-memory `IdempotencyStore` implementation with configurable TTL (default 5 minutes). Production deployments SHOULD use Redis or similar persistent store.
  - Acceptance: duplicate request within TTL MUST return the previously computed result with HTTP 200 and include the header `X-Idempotency-Status: replay`. Tests MUST cover duplicate detection, TTL expiry, and the presence of the `X-Idempotency-Status` header. Implementations MAY provide an alternative behavior (e.g., `409 Conflict`) only via an explicit, documented exception in the PR.

 - FR-002-004: CI Quality Gates
  - Enforce unit tests, ArchUnit rules and mutation testing for this feature. The feature core classes (package `io.forest.integrationhub.integrity`) SHOULD meet a mutation score of >= 80% as a feature goal.
  - Acceptance: CI pipeline passes with tests green and Pitest score at-or-above target for feature-core.

  Note: The project constitution mandates a Core module mutation threshold of >= 90%; feature-level goals MUST align with constitution unless a documented exception is granted.

## Success Criteria

- Unit tests for `io.forest.integrationhub.integrity` pass in CI.
- Test vectors are present in `specs/002-data-integrity/test-vectors/` and used by tests.
- Pitest mutation score for the feature-core >= 80% (document any justified exceptions in the PR description).

## User Stories

- As an Integration Hub operator, I want invalid RPC messages rejected early so downstream systems are not corrupted.
- As a developer, I want deterministic checksum utilities so I can detect tampering and verify payloads.
- As an API client, I want idempotent request handling so retries don't cause duplicate side effects.

## Edge Cases

- Empty or null string fields, very large payloads, unknown/forward-compatible proto fields, and unexpected numeric ranges.

## Mapping to Tasks

- T002-001 → FR-002-001
- T002-002 → FR-002-001
- T002-003 → FR-002-002
- T002-004 → FR-002-003
- T002-005 → FR-002-004

