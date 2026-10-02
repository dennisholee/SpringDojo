@PoC-MIG
Feature: MIG - MongoDB to Distributed SQL migration

  Scenario: A backfill closes the legacy gap
    Given the legacy store is available
    And the legacy store holds 3 migration customers
    When I run the migration backfill
    Then the migration gap is 0
    And the CDM holds at least 3 migration parties

  Scenario: A dual-write keeps both stores consistent
    Given the legacy store is available
    When I dual-write a migration customer named "MIG-Dual"
    Then the migration gap is 0
    And the CDM holds a migration party named "MIG-Dual"

  Scenario: Reconciliation applies a legacy-side change
    Given the legacy store is available
    And the legacy store is fully reconciled
    And I record the migration gap
    When the legacy system adds a migration customer named "MIG-Appended"
    Then the migration gap has increased by 1
    When I run the migration reconciliation
    Then the migration gap is 0
    And the CDM holds a migration party named "MIG-Appended"

  Scenario: Backfill and reconciliation are idempotent
    Given the legacy store is available
    When I dual-write a migration customer named "MIG-Idem"
    And I run the migration backfill
    And I run the migration reconciliation
    Then the migration gap is 0
    And the CDM holds exactly 1 migration party named "MIG-Idem"
