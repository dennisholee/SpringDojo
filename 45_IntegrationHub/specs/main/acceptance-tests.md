# Acceptance Tests — Quality / ArchUnit Enforcement

This document shows the automated and manual validation steps that demonstrate the feature meets the success criteria.

## Automated tests

1. ArchUnit test suite (unit tests)

   - Run: `mvn -Dtest=ArchUnitRulesTest test`
   - Expected: Tests pass for passing samples; tests fail when seeded violations are present.

2. CI integration

   - CI job runs the ArchUnit test suite and fails the pipeline on any violations. The CI artifact should include the test report.

## Manual validation

1. Seed a violation

   - Add a sample class under `src/test/java/archunit-samples/failing` that imports an adapter class from `io.forest.integrationhub.adapters.web`.
   - Run local ArchUnit test — verify it fails and the report includes the violating class and a remediation hint.

2. Remediate

   - Follow remediation guide: extract a `Port` interface and implement adapter in the adapters module. Re-run tests and verify pass.

## Example Given/When/Then acceptance criteria

1. **Given** a PR that introduces a direct core→adapter dependency, **When** CI runs, **Then** the build fails and the ArchUnit report lists the offending class and suggested remediation.

2. **Given** a developer fixes the violation by introducing a `Port` interface and moving adapter code to the adapter module, **When** CI re-runs, **Then** the build passes and a CI artifact demonstrates no violations.

## Test data and samples

- Add sample passing and failing classes under `src/test/resources/archunit-samples/` and reference them in the ArchUnit tests.  
