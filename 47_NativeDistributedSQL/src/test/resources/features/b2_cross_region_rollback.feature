@PoC-B2
Feature: B2 - Cross-region rollback

  Scenario: A simulated failure after cross-region writes rolls everything back
    When I call the cross-region rollback demo for "Rollback Ltd" with account "HK-E2E-5"
    Then the rollback demo reports rolled back is true
    And the rollback demo snapshots are identical

  Scenario: A real constraint violation aborts the whole cross-region transaction
    Given a UK party named "Duplicate Ltd" exists
    And a product holding "SAVINGS_ACCOUNT" numbered "HK-E2E-DUP" exists for that party
    And I record the party row count
    When I onboard a cross-region customer named "Duplicate Ltd" with account "HK-E2E-DUP"
    Then the response status is 500
    And the party row count is unchanged
