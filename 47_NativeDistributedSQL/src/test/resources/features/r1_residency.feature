@PoC-R1
Feature: R1 - Data residency

  Scenario: No row leaves its home region
    Given a UK party named "Residency Ltd" exists
    When I onboard a cross-region customer named "Residency XB Ltd" with account "HK-E2E-R1"
    Then every party row is homed in region "uk"
    And every relationship row is homed in region "uk"
    And every contact_point row is homed in region "uk"
    And every product_holding row is homed in region "hk"
    And the voter replicas of the party table are pinned to region "uk"
    And the voter replicas of the product_holding table are pinned to region "hk"
