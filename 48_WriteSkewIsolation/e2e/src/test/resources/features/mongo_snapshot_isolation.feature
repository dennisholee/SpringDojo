@engine-mongo
Feature: Snapshot isolation permits write skew

  Scenario: concurrent resignations commit without conflict but break the invariant
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign as signatories
    Then both requests committed with no error
    # WRITE SKEW: no serial order of the two transactions could have produced this state
    And 0 authorized signatories remain

  Scenario: a hot-row counter closes the gap (application-level mitigation)
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign against a guarded party
    Then exactly one resigns
    And 1 authorized signatory remains
