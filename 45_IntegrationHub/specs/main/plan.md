# Implementation Plan — Quality / ArchUnit Enforcement

This plan describes the incremental implementation approach to add ArchUnit enforcement for hexagonal boundaries.

## Overview

- Goal: Enforce package layering and prevent domain/core from depending on adapters or framework code.  
- Timebox: Deliver a minimal enforcement set (critical rules + CI enforcement) in the first increment.  

## Milestones

1. **M0 — Developer foundation** (this change)
   - Create spec and rule docs. Provide sample passing/failing cases and a test harness skeleton.

2. **M1 — Rule implementation**
   - Implement first-pass ArchUnit rules: no core→adapter, no Spring annotations in core, layering checks.
   - Add unit tests and sample code.

3. **M2 — CI enforcement**
   - Add CI step to run ArchUnit tests, fail PRs on violations, and publish test reports.

4. **M3 — Developer experience**
   - Add remediation guide, allowlist/exception process, and onboarding examples.

## Risks & Mitigations

- Risk: False positives block development.  
  Mitigation: Start with conservative rules, include pass/fail samples, and provide allowlist process.  
- Risk: Long run-time for tests.  
  Mitigation: Limit checks to compiled classes and cache results in CI when possible.

## Acceptance

- CI fails on seeded violations and provides clear remediation hints.  
- Developers can reproduce failures locally with a single Maven command.
