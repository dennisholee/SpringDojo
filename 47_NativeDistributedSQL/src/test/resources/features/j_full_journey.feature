@PoC-J
Feature: J - Full front-to-back journey

  Scenario: A customer 360 view is consistently readable across the UK and Hong Kong
    When I onboard a cross-region customer named "Journey Ltd" with account "HK-E2E-6"
    Then the customer 360 read for the onboarded party is complete across all four domains
    And the customer 360 read homes the party in "uk" and the product holding in "hk"
