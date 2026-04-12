# Feature Specification: Quality — Enforce Hexagonal Boundaries

**Feature Branch**: `quality/archunit-enforcement`  
**Created**: 2026-04-12  
**Status**: Draft  
**Input**: User description: "Title: Quality. Context: Enforce hexagonal boundaries and modular encapsulation through automated checks. Users: developers and build engineers. Scenarios: 1) Build-time checks fail when middleware logic leaks into core; 2) New module added -> must pass encapsulation rules. Functional requirements: ArchUnit rules, tests, CI gate enforcement. Constraints: minimal false positives, clear failure messages. Success criteria: CI fails on violations, clear remediation steps, sample failing/passing cases. Desired outputs: spec.md, archunit-rules.md, acceptance-tests.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Prevent Core Leakage (Priority: P1)

As a developer, I must be prevented from introducing direct dependencies from the domain/core packages to adapter or framework code so that domain code remains framework-agnostic and easily testable.

**Why this priority**: Preventing core leakage preserves long-term maintainability and enables safe refactors. It is prerequisite for all other integration hub features.

**Independent Test**: Run the ArchUnit rules in CI or locally; if any rule fails, the build fails and shows the offending classes.

**Acceptance Scenarios**:

1. **Given** a new class in `io.forest.integrationhub.core`, **When** it depends on `io.forest.integrationhub.adapters.web`, **Then** ArchUnit test fails with a clear violation message.
2. **Given** a refactor that moves an adapter-dependent class into core, **When** CI runs, **Then** the PR is blocked until the dependency is removed or moved to an adapter.

---

### User Story 2 - Clear Failure Messaging (Priority: P2)

As a build engineer, I need the rule failures to include remediation steps and examples so developers can quickly resolve violations.

**Why this priority**: Faster remediation reduces cycle time and reduces noise from false positives.

**Independent Test**: Inspect a sample violation artifact that includes the class, the offending dependency, and a recommended fix (e.g., introduce a port interface or mapper).

**Acceptance Scenarios**:

1. **Given** an ArchUnit failure, **When** a developer clicks the CI link, **Then** the failure report shows the violating class, the forbidden dependency, and a short remediation hint.

---

### User Story 3 - Developer Onboarding (Priority: P3)

As a new developer, I want a short guide and examples showing common violations and fixes so I can adopt the architecture quickly.

**Why this priority**: Onboarding reduces costly mistakes and improves development velocity.

**Independent Test**: A developer can follow the remediation guide to fix a seeded failing example and verify the build passes locally.

**Acceptance Scenarios**:

1. **Given** a seeded failing example in `src/test/java/io/forest/integrationhub/core/BadCoreClass.java`, **When** a developer applies the remediation pattern from the guide, **Then** local checks pass.

---

### Edge Cases

- Generated sources (codegen) may introduce indirect dependencies; generated packages should be whitelisted or validated separately.  
- Test-only dependencies may transiently violate rules; tests must be excluded or rules adjusted accordingly.  
- Multi-module projects with shared infra modules must declare explicit allowed-deps lists.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide ArchUnit rules that enforce package layering (core ← ports ← adapters).  
- **FR-002**: The system MUST run the rules in CI and fail builds on violations.  
- **FR-003**: The system MUST produce readable failure reports including class name, offending dependency, and remediation hint.  
- **FR-004**: The system MUST include a small set of sample passing and failing code examples for developer onboarding.  
- **FR-005**: The system MUST allow an allowlist/whitelist mechanism for exceptional, documented cases.  
- **FR-006**: The system MUST provide a remediation guide with patterns for common fixes (ports, mappers, DTOs).  

### Non-Functional Requirements

- **NFR-001**: Rule evaluation must complete in under 30s on a developer workstation for the repo size.  
- **NFR-002**: False positive rate must be minimal; any new rule must include pass/fail examples.  

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: CI fails when ArchUnit detects core→adapter dependency violations in a PR.  
- **SC-002**: Developer can run local check (`mvn -Darchunit.root.package=io.forest.integrationhub -Dtest=io.forest.integrationhub.ArchUnitRulesTest test`) and receive the same failure report as CI.  
- **SC-003**: Onboarding guide includes a deterministic remediation walkthrough that resolves the seeded violation in <= 30 minutes when executed locally with the documented command sequence.  
- **SC-004**: Sample failing/passing cases included in repo and referenced by CI artifacts.  
- **SC-005**: Mutation testing thresholds are enforced: Core modules achieve >= 90% mutation score; Adapter modules achieve >= 80%. Failure to meet thresholds fails CI and is reported in the CI artifact (Pitest report).  
- **SC-006**: Cyclomatic complexity ceilings are enforced: Core methods <= 6, Adapter/utility methods <= 10. Complexity checks run in CI (PMD/Checkstyle/Sonar) and failures block merges.

## Assumptions

- The project uses Java + Spring Boot + Maven.  
- Core packages live under `io.forest.integrationhub.core` and adapters live in separate packages (web, messaging, rpc, file, legacy).  
- CI can run Maven goals and publish test output.

## Dependencies

- Adding ArchUnit dependency to `pom.xml`.  
- CI pipeline configuration to execute the ArchUnit tests and fail the build.

## Deliverables

- `specs/main/spec.md` (this file)  
- `specs/main/archunit-rules.md` (concrete rules + remediation)  
- `specs/main/acceptance-tests.md` (automated + manual validation)  
- `specs/main/tasks.md` (implementation tasks and checklist)  
- `specs/main/plan.md` (implementation plan / milestones)

## Annex — Implementation-agnostic summary (consolidated)

The following material was consolidated from an earlier, implementation-agnostic draft (`specs/001-quality`) to preserve high-level intent while keeping `specs/main` as the canonical Quality specification.

- **Summary**: Enforce hexagonal boundaries and modular encapsulation through automated checks. Prevent middleware/adapter logic from leaking into the core/domain layer; block merges on violations and provide clear remediation guidance.
- **Edge Cases**:
	- Allow documented exceptions for migration helpers that must temporarily live in `core`; these require an explicit allowlist entry and owner.
	- Generated or vendored code that shares package name patterns should be excluded via an allowlist and validated separately.
	- Test-only dependencies that intentionally reference adapters should be excluded or handled with test-scoped rules.
- **Assumptions**:
	- The repository can run CI checks that execute fitness functions during the standard test phase.
	- The enforcement approach focuses on detection and remediation guidance; tool-specific implementations (ArchUnit) are described in this canonical spec and the constitution.

Consolidation note: `specs/001-quality` has been merged into this file; the original directory was archived/removed to avoid duplication.
