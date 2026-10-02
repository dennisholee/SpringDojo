package io.forest.isolation.signatory;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scenarios are engine-agnostic: the {@code @engine-*} tag selects the adapter, and every step
 * then works purely through the shared {@link SignatoryStore} port and {@link SignatoryGuard} rule.
 *
 * <p>Each scenario also emits its own evidence trail — the rows before and after, the concurrent
 * transaction trace and the verdict — through {@link Scenario#log}, so the proof is captured in the
 * Cucumber JSON/HTML reports. {@link EvidenceReport} then rolls every verdict up into one table.
 */
public class SignatorySteps {

    private SignatoryStore store;
    private Runnable reset;
    private Supplier<String> snapshot;
    private Outcome outcome;
    private String engine;
    private String isolation;

    /**
     * The scenario is captured in the hook because Cucumber's arity check rejects {@link Scenario}
     * as a step-definition parameter; hooks are the supported injection point.
     */
    private Scenario scenario;

    @Before
    public void selectEngine(Scenario scenario) {
        this.scenario = scenario;
        var tags = scenario.getSourceTagNames();
        if (tags.contains("@engine-mongo")) {
            store = MongoFixture.store();
            reset = MongoFixture::reset;
            snapshot = MongoFixture::snapshot;
            engine = "MongoDB (replica set)";
            isolation = "snapshot";
        } else if (tags.contains("@engine-cockroach")) {
            store = CockroachFixture.store();
            reset = CockroachFixture::reset;
            snapshot = CockroachFixture::snapshot;
            engine = "CockroachDB";
            isolation = "strict serializable";
        } else {
            throw new IllegalStateException("Scenario is missing an @engine-* tag: " + scenario.getName());
        }
    }

    @Given("party P1 has 2 authorized signatories")
    public void partyHasTwoAuthorizedSignatories() {
        reset.run();
        scenario.log("BEFORE   " + snapshot.get());
    }

    @When("alice and bob concurrently resign as signatories")
    public void concurrentlyResign() {
        outcome = ConcurrentRevoke.run(store, false);
        logRequestAndTrace();
    }

    @When("alice and bob concurrently resign against a guarded party")
    public void concurrentlyResignAgainstGuardedParty() {
        outcome = ConcurrentRevoke.runGuarded(store);
        logRequestAndTrace();
    }

    @When("alice and bob concurrently resign with retry-on-serialization")
    public void concurrentlyResignWithRetry() {
        outcome = ConcurrentRevoke.run(store, true);
        logRequestAndTrace();
    }

    /** Captures the request and the exact interleaving each side performed, in both threads. */
    private void logRequestAndTrace() {
        scenario.log("REQUEST  two concurrent inTransaction(resign) calls, forced to read before either writes");
        for (String line : outcome.trace()) {
            scenario.log("TRACE    " + line);
        }
    }

    @Then("both requests committed with no error")
    public void bothCommittedWithoutError() {
        assertEquals(2, outcome.commits(), "both transactions should have committed");
        assertNull(outcome.aliceError(), "alice should not have errored");
        assertNull(outcome.bobError(), "bob should not have errored");
    }

    @Then("exactly one commits")
    public void exactlyOneCommits() {
        assertEquals(1, outcome.commits(), "exactly one transaction should commit");
    }

    @Then("exactly one resigns")
    public void exactlyOneResigns() {
        assertEquals(1, outcome.resignations(), "exactly one resignation should take effect");
    }

    @Then("exactly one resigns and one is refused on the retry")
    public void oneResignsAndOneIsRefusedOnRetry() {
        assertEquals(1, outcome.resignations(), "exactly one retried resignation should take effect");
        assertTrue(ConcurrentRevoke.isSerializationFailure(outcome.failure()),
                "the refused party should have been aborted with SQLSTATE 40001 before its retry");
    }

    @Then("^the other fails with a serialization error \\(SQLSTATE 40001\\)$")
    public void otherFailsWithSerializationError() {
        assertNotNull(outcome.failure(), "the losing transaction should have failed");
        assertTrue(ConcurrentRevoke.isSerializationFailure(outcome.failure()),
                "the failure should be a SQLSTATE 40001 serialization error");
    }

    @Then("^([0-9]+) authorized signator(?:y|ies) remain(?:s)?$")
    public void authorizedSignatoriesRemain(int expected) {
        assertEquals(expected, outcome.finalAuthorized(), "unexpected number of authorized signatories");
    }

    /**
     * The decision-maker evidence for the scenario: what the run left in the database, how the
     * engine answered each request, and the verdict — attached to the Cucumber report and rolled up
     * into the consolidated verdict table.
     */
    @After
    public void captureEvidence(Scenario scenario) {
        if (outcome == null) {
            return;                       // the scenario failed before it reached the concurrent step
        }
        String response = describeResponse(outcome);
        String verdict = verdict(outcome);
        scenario.log("AFTER    " + snapshot.get());
        scenario.log("RESPONSE " + response);
        scenario.log("VERDICT  " + verdict);
        EvidenceReport.record(engine, isolation, scenario.getName(), response,
                outcome.finalAuthorized(), verdict);
    }

    @AfterAll
    public static void writeVerdictTable() {
        EvidenceReport.write();
    }

    private static String describeResponse(Outcome outcome) {
        String committed = outcome.commits() + "/2 committed";
        return outcome.failure() == null ? committed
                : committed + ", abort=" + ConcurrentRevoke.describe(outcome.failure());
    }

    /** A plain-language verdict derived from the observed end state, never from the expectation. */
    private String verdict(Outcome outcome) {
        if (outcome.finalAuthorized() < SignatoryGuard.MIN_AUTHORIZED) {
            return "WRITE SKEW - invariant broken (" + outcome.finalAuthorized()
                    + " authorized left, both transactions committed)";
        }
        if (engine.startsWith("MongoDB")) {
            return "PREVENTED - app-level hot-row counter; one transaction hit a write conflict ("
                    + outcome.finalAuthorized() + " left)";
        }
        String state = outcome.failure() == null ? "n/a" : ConcurrentRevoke.sqlState(outcome.failure());
        return "PREVENTED - strict serializable; one transaction aborted with SQLSTATE " + state
                + " (" + outcome.finalAuthorized() + " left)";
    }
}
