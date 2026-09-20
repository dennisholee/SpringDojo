@PoC-A1
Feature: A1 - Region-local onboarding in the United Kingdom

  Scenario: A UK-only customer is onboarded in a single region-local transaction
    When I onboard a UK-only customer named "Tan Wei Ltd"
    Then the response status is 200
    And the onboarding pattern is "A_REGION_LOCAL"
    And the response carries a party, relationship and contact point id
    And the response carries no product holding id
    And the relationship and contact point reference the returned party
    And every party row is homed in region "uk"
    And every relationship row is homed in region "uk"
    And every contact_point row is homed in region "uk"
