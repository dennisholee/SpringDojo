@PoC-M
Feature: M - No compensation machinery

  Scenario: Compensation endpoints do not exist
    Then posting to "/api/try" returns 404
    And posting to "/api/confirm" returns 404
    And posting to "/api/cancel" returns 404

  Scenario: No compensation bookkeeping tables exist
    Then the database has no table whose name contains "saga" or "outbox"
