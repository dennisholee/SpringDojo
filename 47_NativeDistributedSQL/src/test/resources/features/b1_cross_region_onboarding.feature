@PoC-B1
Feature: B1 - Cross-region onboarding

  Scenario: A customer spanning the UK and Hong Kong is onboarded in one transaction
    When I onboard a cross-region customer named "Cross Border Ltd" with account "HK-E2E-3"
    Then the response status is 200
    And the onboarding pattern is "B_CROSS_REGION"
    And the response carries a party, relationship and contact point id
    And the response carries a product holding id
    And the onboarded party is homed in region "uk"
    And the onboarded product holding is homed in region "hk"

  Scenario: Region-local and cross-region onboarding are both timed for the report
    When I time a UK-only onboarding named "Timing UK Ltd"
    And I time a cross-region onboarding named "Timing XB Ltd" with account "HK-E2E-4"
    Then both onboarding timings are recorded in the report
    And the cross-region onboarding is at least as slow as the region-local one
