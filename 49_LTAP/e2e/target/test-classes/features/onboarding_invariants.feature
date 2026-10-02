# Onboarding invariants over the lakehouse: what the engine does with the write skew the rule cannot
# prevent. Six scenarios, each one row of the consolidated verdict table:
#
#   @engine-reference  the harness that models snapshot isolation and nothing more. It is in the table
#                      because the anomaly has to be shown somewhere, and because "the engine's commit
#                      protocol decides the verdict" is only a claim until two engines are compared.
#   @engine-iceberg    real MinIO plus a real Iceberg REST catalog, driven through the same port.
#
# Every row is written from the observed end state, so a regression changes a verdict instead of an
# expectation.
#
# O3 to O5 sequence the two commits -- the loser's commit is not even built until the winner's commit
# is in the catalog -- because the engine's refusal is only deterministic in that shape, and that is
# the shape the JUnit isolation test pins. O6 drops that assumption and lets the two commits overlap,
# which is what two independent clients produce; the row then records what the catalog does with the
# race instead of asserting an outcome.
@onboarding
Feature: Onboarding write skew, and what each engine's commit protocol does about it

  # O1 - the anomaly the design has to survive (INV-1: a party must keep a primary contact point). Both
  # transactions read "2 primary contact points" and demote a *different* one, so nothing in either
  # write names a row the other one touched. Snapshot isolation has nothing to detect: both commits
  # land, and no serial order of the two transactions could have produced the state that is left.
  @engine-reference
  Scenario: two concurrent demotions of different primary contact points both commit
    Given party P1 is onboarded with 2 primary contact points
    When alice and bob concurrently demote a different primary contact point
    Then both demotions commit with no conflict
    # WRITE SKEW: 2 primary contact points in, 0 out
    And the party is left with 0 primary contact points

  # O2 - the mitigation, on the engine that has nothing to detect: put a write to the party's shared
  # counter in front of the rule. The demotions stay disjoint, but the counter is one key, so the
  # losing commit is refused.
  @engine-reference
  Scenario: the serialization point closes the gap
    Given party P1 is onboarded with 2 primary contact points
    When alice and bob concurrently demote against a guarded party
    Then exactly one demotion commits and the other is refused
    And the party is left with 1 primary contact point
    And the party's counter records exactly one increment

  # O3 - the same interleaving against the real lakehouse. Measured, not assumed: the cross-table commit
  # names the contact_point snapshot each transaction read, so the catalog refuses the loser even
  # though the two demotions are disjoint. INV-1 holds here for a reason nobody wrote for INV-1.
  @engine-iceberg
  Scenario: the same interleaving against the lakehouse's cross-table commit
    Given party P1 is onboarded with 2 primary contact points
    When alice and bob concurrently demote a different primary contact point
    Then the winning demotion commits and the loser is refused with a commit conflict
    And the party is left with 1 primary contact point

  # O4 - the guard on the real lakehouse, for the same reason O3 is here: it is worth knowing whether the
  # serialization point changes anything where the commit protocol already refuses the loser. Measured,
  # it does not -- the counter column is the portable half of the design, not the lakehouse half.
  @engine-iceberg
  Scenario: the serialization point on the lakehouse
    Given party P1 is onboarded with 2 primary contact points
    When alice and bob concurrently demote against a guarded party
    Then the winning demotion commits and the loser is refused with a commit conflict
    And the party is left with 1 primary contact point
    And the party's counter records exactly one increment

  # O5 - the shape the REST edge actually produces. A refused submission is answered 409 with the
  # contract "nothing was committed, retry", so the retry reads fresh state -- and now sees one primary
  # contact point, so the rule declines the demotion itself. The invariant survives a third way, and the
  # retry writes nothing at all.
  @engine-iceberg
  Scenario: the refused demotion retried with a fresh read is declined by the rule
    Given party P1 is onboarded with 2 primary contact points
    When alice and bob concurrently demote a different primary contact point
    And the refused demotion is retried with a fresh read
    Then the winning demotion commits and the loser is refused with a commit conflict
    And the retry was declined by the rule, not by the catalog
    And the party is left with 1 primary contact point
    And the retry wrote nothing to the tables

  # O6 - the same two requests with the commits allowed to overlap, which is what two independent
  # clients produce. The row records which of the two things the catalog did -- refuse one commit, or
  # accept both and let one demotion vanish -- and only the invariant is pinned, so a changed verdict is
  # a finding about the engine rather than a broken build.
  @engine-iceberg
  Scenario: the two commits are allowed to overlap
    Given party P1 is onboarded with 2 primary contact points
    And the two commits are allowed to overlap
    When alice and bob concurrently demote a different primary contact point
    Then the party is left with 1 primary contact point
