# Project Constitution

## Core Principles

### I. Test-First (NON-NEGOTIABLE)
All new feature work MUST include failing tests that demonstrate the desired behavior before implementation. Tests must be present in the feature branch and in CI pipelines.

### II. Quality Gates (MUST)
Critical features MUST include unit tests and automated quality checks. Mutation testing, static analysis (Checkstyle/ArchUnit) and test execution are required in CI; any exception requires explicit justification in the PR.

### III. Backwards Compatibility (SHOULD)
Changes to public contracts (protos, APIs) SHOULD be accompanied by a migration plan and versioning notes.

### IV. Observability (SHOULD)
Libraries MUST emit structured logs for critical operations and provide clear failure messages for validation routines.

## Governance
This constitution supersedes informal practices. Amendments require an authored rationale and a review by repository maintainers.

**Version**: 1.0 | **Ratified**: 2026-04-13
