# ArchUnit Allowlist

This file documents explicit exceptions to the ArchUnit rules enforced by CI.

Format (YAML-style plain text entries):

- id: AL-001
  class: io.forest.integrationhub.legacy.LegacyBridge
  rule: No core→adapter dependency
  reason: Temporary migration wrapper until adapter refactor completed
  owner: team-infra
  approved_by: architecture-owner
  expires: 2026-06-01

How to use
- Add one entry per allowed exception with a clear justification, owner and expiry.
- Any addition must include a short remediation plan and a review date.
- CI/validation: CI should surface allowlist entries and limit automated failures to undocumented exceptions.

Maintenance
- Review allowlist entries monthly (or per release cadence). Remove entries once migration/repair is complete.

Notes
- This file is intentionally small and human-readable. Each entry is an explicit, timeboxed exception to an otherwise blocking fitness function.
