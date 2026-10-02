# ADR-001: Choose "At Least One Authorized Signatory" as the Invariant

## Problem statement
Which invariant best demonstrates write skew in a way that is (a) a genuine CDM governance rule,
(b) an absence/count constraint unprotectable by a unique index, and (c) a structural clone of the
canonical write-skew example for pedagogical clarity?

## Background
The PoC continues the customer-data-master (CDM) domain. A party (customer) must always keep at
least one authorized signatory — an individual empowered to act on the party's accounts. Losing the
last signatory leaves the customer unable to operate. The phenomenon being demonstrated (write skew)
only manifests on constraints that cannot be enforced by a unique index.

## Constraints
- Must be a count/absence constraint — no unique index can protect it.
- Must be expressible as a pure function over a minimal read/write surface (engine-agnostic).
- Must be a real governance rule, not a contrived database example.
- One party (`P1`) with two authorized signatories (`alice`, `bob`) must suffice.

## Assumptions
- A single party with exactly two signatories is enough to reproduce the anomaly.
- The rule is enforced in application code, not by the schema.

## Options

| | At-least-one signatory (count) | At-most-one primary (count) | Unique index (presence) | Balance ≥ 0 (aggregate) |
| --- | --- | --- | --- | --- |
| Summary | Revoke is refused when it would drop the count below 1 | Enforce a single primary via a count check | A unique key already prevents duplicates | A non-negative running total |
| Write-skew territory | Yes — absence constraint | Yes | No — a unique index protects it | Yes |
| CDM fit | Direct governance rule | Contrived | N/A | Finance-flavoured, off-domain |
| Canonical clone | Doctor-on-call | — | — | — |
| Score (out of 5) | 5 | 3 | 1 | 3 |
| Remarks | Chosen. Proven by concurrent resignation of `alice` and `bob`. | Valid, but less natural. | Rejected: defeats the purpose. | Rejected: off-domain. |

## Consequences
- The rule is a single `if (count <= MIN_AUTHORIZED) return false;` — trivially reviewable.
- Both signatories belong to the same party, so the invariant is per-party (see
  [ADR-006](ADR-006-sharding-does-not-change-outcome.md)).
- The invariant is an absence constraint, so the schema cannot enforce it — the whole point of the
  PoC.
