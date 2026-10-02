@engine-cockroach
Feature: Strict serializable prevents write skew

  Scenario: concurrent resignations are serialised and the invariant holds
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign as signatories
    Then exactly one commits
    And the other fails with a serialization error (SQLSTATE 40001)
    # WRITE SKEW PREVENTED: the read-write dependency cycle was detected and one side aborted
    And 1 authorized signatory remains

  Scenario: a retry loop preserves the invariant and still succeeds
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign with retry-on-serialization
    Then exactly one resigns and one is refused on the retry
    And 1 authorized signatory remains
