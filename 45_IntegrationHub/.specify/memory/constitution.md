# [PROJECT_NAME] Constitution

This Constitution codifies the project's Core Technical Identity: architecture-as-code, evolutionary design, and automated governance. Architectural boundaries are not suggestions — they are executable policy enforced at build time. This document defines the fitness functions that the CI pipeline will exercise to ensure the system remains fit for change.

Core Technical Identity
- Architecture as Code: Boundaries are encoded as tests and build rules; the build enforces them.
- Evolutionary Design: The system is designed to be changed safely; fitness functions enable evolution without structural regressions.
- Automated Governance: CI is the primary enforcement plane — policy is automated, repeatable, and fails fast.

## Guiding Principles of Architectural Fitness

This project adopts an "Architectural Guardian" posture: vivid, systems-engineering language that turns architectural intent into automated, measurable constraints.

1. Build-Time Enforcement
- Statement: All architectural boundaries are executable. Fitness functions run during the normal test phase; any violation fails the build.
- Rationale: Shifts detection left; prevents architectural decay from being merged.

2. Zero Structural Drift
- Statement: Declared architecture and code structure must remain congruent. Structural drift is a guarded exception and must be corrected immediately.
- Rationale: Predictability of structure is essential for safe evolution and low-cost refactoring.

3. Observable Integrity
- Statement: Structural health is observable. Key metrics—architecture test pass/fail, mutation score, cyclomatic ceilings, and adapter compatibility—are first-class telemetry.
- Rationale: You cannot govern what you cannot measure; observability enables automated governance.

4. Evolutionary Design
- Statement: Fitness functions are designed to enable safe change. They prevent anti-patterns while allowing deliberate refactorings and API evolution when accompanied by tests and migration plans.
- Rationale: Support continuous change without eroding structural guarantees.

5. Automated Governance
- Statement: Automated checks are the primary gate. Human review supplements but does not substitute for failing fitness functions.
- Rationale: Scale and consistency require automation; CI acts as the policy enforcement plane.

## The Fitness Function Law (ArchUnit)

Law: Structural constraints are encoded as ArchUnit fitness functions. The test-suite is the single source of truth for permitted structure. Any change that causes a fitness-function failure is a policy violation and must be remediated prior to merge.

Mandated ArchUnit fitness functions (minimal set):

- Inward Dependency Rule (Domain cannot see Infrastructure)
	- Test name: `InwardDependencyRuleTest`
	- Requirement: Classes in the Core/Domain layer MUST NOT depend on classes in Infrastructure, Adapters, or technical frameworks. Dependencies flow inward only via Ports.
	- Enforcement: ArchUnit rule asserting domain packages have no dependencies on adapter/infra packages; failure blocks CI.

- Package-Private Encapsulation (Adapters cannot see Domain internals)
	- Test name: `PackagePrivateEncapsulationTest`
	- Requirement: Domain internals remain package-private unless intentionally exported via a well-defined Port. Adapters must not access package-private internals or circumvent encapsulation (no reflective hacks without documented exception).
	- Enforcement: ArchUnit assertions combined with a `@Port` export convention; only `@Port`-annotated types may be referenced externally.

- No-Spring Domain (The Core must have zero org.springframework imports)
	- Test name: `NoSpringInDomainTest`
	- Requirement: The Core/Domain module must be framework-agnostic — zero imports or transitive references to `org.springframework.*` in domain packages.
	- Enforcement: ArchUnit rule that asserts absence of Spring packages from domain classes; any leakage fails CI.

Operational rules:
- All ArchUnit tests live in a dedicated test package and execute during the standard test phase.
- Failure messages must identify the violated rule and offending classes/packages.
- Violations are treated as build-blocking defects and must be fixed before merge.

## Functional & Modular Integrity

Java 21 language features are part of our fitness strategy. The following checks are mandatory for the Core module.

- Records for DTOs
	- Rule: DTOs in public API/transport packages must be declared as Java `record` types unless an explicit, documented exception exists.
	- Enforcement: Reflection-based tests assert DTO classes are records; exceptions require PR justification and an explicit 1–2 week exception ticket.

- Sealed Classes for Domain States
	- Rule: Domain state models and bounded ADTs must use `sealed` classes/interfaces where appropriate to enable exhaustive handling and prevent accidental extension.
	- Enforcement: Reflection tests assert sealed declarations in domain-state packages; mutable, non-sealed state requires explicit documented justification.

- Tell, Don't Ask (Behavior Encapsulation)
	- Rule: Business behavior belongs in the domain. Orchestrators and application services must invoke intent-revealing operations on domain objects rather than extracting state to compute behavior.
	- Enforcement: Static-analysis heuristics implemented as tests: detect orchestration classes calling many accessors on domain types or performing aggregation logic that the domain should own. Flagged cases require refactoring or documented exception.

## Swappable Adapter Validation

Adapters implement Ports and must be interchangeable without code changes.

- Compatibility Tests (Contract Test Suite)
	- Requirement: Core provides an adapter contract-test harness validating contract conformance, idempotency, error semantics, resource cleanup, and absence of hidden side-effects.
	- Each adapter module must run the harness against its implementation under a dedicated Spring profile.
	- CI executes the contract suite for the matrix of supported adapters — failures block merges.

- Profile Swapability
	- Requirement: Swapping adapters is achieved by configuration only (e.g., `spring.profiles.active`). No code changes or conditional wiring permitted.
	- Enforcement: CI matrix runs the same acceptance and contract tests with each adapter profile and validates identical port-level behavior.

## Testing & Coverage Fitness

Test quality is a governance metric — the test-suite must verify behavior, not only exercise code paths.

- Mutation Testing (Pitest)
	- Thresholds: Core modules MUST achieve a mutation score >= 90%. Adapter and non-core modules MUST achieve >= 80% unless a documented exception exists.
	- Enforcement: Pitest runs in CI; scores below threshold fail the build.

- Cyclomatic Complexity
	- Ceiling: Methods in the Core DSL and domain model MUST not exceed cyclomatic complexity of 6. Adapter and utility code ceiling: 10 (documented exceptions permissible but short-lived).
	- Enforcement: PMD/Checkstyle/Sonar rules applied to core packages in CI; violations fail the build.

Implementation notes:
 - ArchUnit tests are parameterized by package-globs to allow repository-specific roots; configure via build properties (recommended: `archunit.root.package`).
- Annotate Ports with `@Port` (or equivalent) to make exports explicit and easier to enforce.
- Configure Pitest to target core packages and exclude generated code.

## Definition of Done (Non-negotiable Checklist)

Before merging any PR that introduces logic or structural change, the author MUST verify:

- Build passes all ArchUnit Fitness Functions; there are no architecture test violations.
- New logic is encapsulated behind a Port; exports are minimized and `@Port`-annotated where applicable.
- The module/adapter is swappable by activating a Spring Profile only (demonstrated by CI matrix job or local run).
- A Fitness Audit was performed for any new external dependency: license, transitive surface, framework leakage risk, and mutation/complexity impact are documented in the PR.
- Mutation score thresholds are met for affected modules.
- Cyclomatic complexity ceilings are not exceeded for modified Core methods.
- Adapter compatibility is validated by running the contract test harness where applicable.
- Documentation updated: design note, module README, and a brief summary in the PR describing how fitness requirements were satisfied.

## Governance and Exceptions

These fitness functions are binding. Exceptional deviations require:

- A short, written architecture-owner approval (PR comment or separate ticket).
- A sunset or remediation plan with a date and owner.

Versioning
- **Version**: 1.0.0 | **Ratified**: [RATIFICATION_DATE] | **Last Amended**: [LAST_AMENDED_DATE]

---

Next actions (implementation): generate ArchUnit test skeletons, reflection checks for records/sealed types, an adapter contract-test harness, and CI configuration to run Pitest and complexity checks. These artifacts will be parameterized by repository package roots; please confirm build tool (Maven or Gradle) and the Core/Domain root package to finalize tests and CI steps.
