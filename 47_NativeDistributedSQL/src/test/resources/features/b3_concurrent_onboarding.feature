@PoC-B3
Feature: B3 - Concurrent cross-region onboarding

  Scenario: Two concurrent cross-region onboardings for one account number commit at most one
    Given I record the party row count
    When I fire two concurrent cross-region onboardings for account "HK-E2E-RACE"
    Then exactly one of the two onboardings succeeded
    And exactly 1 product holding exists for account "HK-E2E-RACE"
    And the party row count increased by exactly 1
