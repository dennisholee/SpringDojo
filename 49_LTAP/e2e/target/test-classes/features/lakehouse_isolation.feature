@engine-iceberg @signatory
Feature: A lakehouse gives ACID commits and snapshot isolation, not serializability

  # L1 - the catalog makes "which snapshot is current" an atomic decision, even though every byte
  # lives on object storage. The commit that must not be lost names the exact file it read, and
  # automatic retry is off, so the loser is refused instead of quietly overwriting the winner.
  Scenario: concurrent writers on one partition are refused, not merged
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign against a guarded party
    Then exactly one commits
    And the other is refused with a commit conflict
    And the counter records exactly one increment
    And 1 authorized signatory remains

  # L2 - disjoint partitions. Snapshot isolation has nothing to detect, so both writes land.
  Scenario: concurrent revocations of different signatories both commit and break the invariant
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign as signatories
    Then both requests committed with no error
    # WRITE SKEW: no serial order of the two transactions could have produced this state
    And 0 authorized signatories remain

  # L3 - the mitigation: put a compare-and-set write in front of the rule, so the two transactions
  # collide even though the resignations themselves never touch the same partition.
  Scenario: a compare-and-set counter closes the gap
    Given party P1 has 2 authorized signatories
    When alice and bob concurrently resign against a guarded party
    Then exactly one resigns
    And 1 authorized signatory remains

  # L4 - the analytical half: the previous snapshot is still readable, so the state the invariant
  # held in can be shown, audited and recovered -- something neither engine in 48_WriteSkewIsolation
  # offers.
  Scenario: the previous snapshot still holds the invariant
    Given party P1 has 2 authorized signatories
    And the current snapshot is remembered
    When alice and bob concurrently resign as signatories
    Then both requests committed with no error
    And the current snapshot has moved on
    And the remembered snapshot shows 2 authorized signatories
    And the current snapshot shows 0 authorized signatories

  # L5 - schema evolution is a metadata-only commit: the Parquet files are untouched, so old
  # snapshots keep reading with their old schema.
  Scenario: a column is added without rewriting a data file
    Given party P1 has 2 authorized signatories
    And the current snapshot is remembered
    When the column tier is added to the table
    Then the table columns are id, party_id, authorized, tier
    And no data file was rewritten
    And the remembered snapshot shows 2 authorized signatories

  # L6 - the analytical half: one projected, filtered scan across parties, which is the question the
  # lakehouse exists to answer.
  Scenario: one scan answers the governance question across parties
    Given parties P1 and P2 have 2 and 1 authorized signatories
    When the authorized signatories are counted by party
    Then the count by party is P1=2 and P2=1
