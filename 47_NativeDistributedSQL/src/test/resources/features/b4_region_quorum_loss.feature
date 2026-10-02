@PoC-B4
Feature: B4 - Region quorum loss

  Scenario: A cross-region onboarding aborts when Hong Kong loses quorum, then recovers
    When the Hong Kong region loses quorum
    And I attempt a cross-region onboarding for account "HK-E2E-QUORUM"
    Then the response status is 500
    When the Hong Kong region is restored
    And the cluster is serving reads again within 180 seconds
    Then no party row was committed for that attempt
    And a later cross-region onboarding succeeds within 180 seconds
