@PoC-A2
Feature: A2 - Region-local product holding in Hong Kong

  Scenario: A product holding is opened for an existing party
    Given a UK party named "Holder Ltd" exists
    When I open a product holding "SAVINGS_ACCOUNT" numbered "HK-E2E-1" for that party
    Then the response status is 200
    And the product holding pattern is "A_REGION_LOCAL"
    And the product holding region is "hk"
    And every product_holding row is homed in region "hk"

  Scenario: A product holding for an unknown party is rejected without writing anything
    Given an unknown party identifier
    And I record the product_holding row count
    When I open a product holding "SAVINGS_ACCOUNT" numbered "HK-E2E-2" for the unknown party
    Then the response status is 422
    And the product_holding row count is unchanged
