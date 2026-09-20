@PoC-RM
Feature: RM - Eventually consistent customer-360 read model

  Scenario: The write path does not maintain the projection
    When I onboard a cross-region customer named "RM-Deferred Ltd" with account "HK-E2E-RM1"
    Then the customer-360 read for the onboarded party is not available yet

  Scenario: The projector converges the 360 view
    When I onboard a cross-region customer named "RM-Converged Ltd" with account "HK-E2E-RM2"
    And I run the read-model projection
    Then the customer-360 read for the onboarded party is complete

  Scenario: The projection converges again after a later change
    When I onboard a cross-region customer named "RM-Later Ltd" with account "HK-E2E-RM3"
    And I open a product holding "SAVINGS_ACCOUNT" numbered "HK-E2E-RM3B" for the onboarded party
    And I run the read-model projection
    Then the customer-360 read for the onboarded party lists 2 account numbers
