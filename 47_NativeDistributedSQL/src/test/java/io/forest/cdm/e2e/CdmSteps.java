package io.forest.cdm.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end step definitions.
 *
 * <p>Every step drives the real application over HTTP (via {@link TestRestTemplate}) and then
 * verifies the outcome against the real CockroachDB cluster (via the application {@link DataSource}).
 * There is no mocking, no stubbing and no in-memory database anywhere in this class.
 *
 * <p>Cucumber creates a fresh instance per scenario, so the fields below are scenario state.
 */
public class CdmSteps {

    private static final String UK = "uk";
    private static final String HK = "hk";
    private static final String METRICS_FILE = "target/e2e-metrics/latency.csv";

    /**
     * Makes every scenario run use fresh account numbers, so the suite can be re-run against a
     * cluster that is being reused rather than recreated.
     */
    private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 6);

    private static final java.util.Set<String> KNOWN_TABLES =
            java.util.Set.of("party", "relationship", "contact_point", "product_holding");

    private final List<Integer> concurrentStatuses = new ArrayList<>();
    private long partyCountBeforeFault;
    private long migrationGapBefore;
    private long ukOnboardingMillis;
    private long crossRegionOnboardingMillis;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> response;
    private UUID partyId;
    private UUID unknownPartyId;
    private UUID onboardedPartyId;
    private UUID onboardedProductHoldingId;
    private final Map<String, Long> rowCountsBefore = new LinkedHashMap<>();

    // ------------------------------------------------------------------ GIVEN

    @Given("a UK party named {string} exists")
    public void aUkPartyNamedExists(String legalName) {
        response = post("/api/onboarding/local", onboardingPayload(legalName, "UK_RETAIL", null, null));
        assertThat(response.getStatusCode().value()).as("party fixture must be created").isEqualTo(200);
        partyId = UUID.fromString(body().path("partyId").asText());
    }

    @Given("an unknown party identifier")
    public void anUnknownPartyIdentifier() {
        unknownPartyId = UUID.randomUUID();
    }

    @Given("a product holding {string} numbered {string} exists for that party")
    public void aProductHoldingExistsForThatParty(String productType, String accountNumber) {
        response = post("/api/product-holdings",
                productHoldingPayload(partyId, productType, accountNumber));
        assertThat(response.getStatusCode().value()).as("product holding fixture").isEqualTo(200);
    }

    @Given("I record the {word} row count")
    public void iRecordTheRowCount(String table) {
        rowCountsBefore.put(table, scalar("SELECT count(*) FROM " + table));
    }

    // ------------------------------------------------------------------ WHEN

    @When("I onboard a UK-only customer named {string}")
    public void iOnboardAUkOnlyCustomer(String legalName) {
        response = post("/api/onboarding/local", onboardingPayload(legalName, "UK_RETAIL", null, null));
        captureOnboardingResult();
    }

    @When("I onboard a cross-region customer named {string} with account {string}")
    public void iOnboardACrossRegionCustomer(String legalName, String accountNumber) {
        response = post("/api/onboarding/cross-region",
                onboardingPayload(legalName, "HK_INSURANCE", accountNumber, "SAVINGS_ACCOUNT"));
        captureOnboardingResult();
    }

    @When("I open a product holding {string} numbered {string} for that party")
    public void iOpenAProductHolding(String productType, String accountNumber) {
        response = post("/api/product-holdings", productHoldingPayload(partyId, productType, accountNumber));
    }

    @When("I open a product holding {string} numbered {string} for the unknown party")
    public void iOpenAProductHoldingForTheUnknownParty(String productType, String accountNumber) {
        response = post("/api/product-holdings", productHoldingPayload(unknownPartyId, productType, accountNumber));
    }

    @When("I call the cross-region rollback demo for {string} with account {string}")
    public void iCallTheRollbackDemo(String legalName, String accountNumber) {
        response = post("/api/onboarding/cross-region/rollback-demo",
                onboardingPayload(legalName, "HK_INSURANCE", accountNumber, "SAVINGS_ACCOUNT"));
    }

    @When("I time a UK-only onboarding named {string}")
    public void iTimeAUkOnlyOnboarding(String legalName) {
        long startedAt = System.nanoTime();
        response = post("/api/onboarding/local", onboardingPayload(legalName, "UK_RETAIL", null, null));
        ukOnboardingMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        recordLatency("A_REGION_LOCAL", ukOnboardingMillis);
    }

    @When("I time a cross-region onboarding named {string} with account {string}")
    public void iTimeACrossRegionOnboarding(String legalName, String accountNumber) {
        long startedAt = System.nanoTime();
        response = post("/api/onboarding/cross-region",
                onboardingPayload(legalName, "HK_INSURANCE", accountNumber, "SAVINGS_ACCOUNT"));
        crossRegionOnboardingMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        recordLatency("B_CROSS_REGION", crossRegionOnboardingMillis);
    }

    // ------------------------------------------------------------------ THEN

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expected) {
        assertThat(response.getStatusCode().value())
                .as("response body: " + response.getBody())
                .isEqualTo(expected);
    }

    @Then("the onboarding pattern is {string}")
    public void theOnboardingPatternIs(String expected) {
        assertThat(body().path("pattern").asText()).isEqualTo(expected);
    }

    @Then("the response carries a party, relationship and contact point id")
    public void theResponseCarriesCoreIds() {
        JsonNode json = body();
        assertThat(json.path("partyId").asText()).isNotBlank();
        assertThat(json.path("relationshipId").asText()).isNotBlank();
        assertThat(json.path("contactPointId").asText()).isNotBlank();
        onboardedPartyId = UUID.fromString(json.path("partyId").asText());
    }

    @Then("the response carries no product holding id")
    public void theResponseCarriesNoProductHoldingId() {
        JsonNode id = body().path("productHoldingId");
        assertThat(id.isNull() || id.asText().isBlank()).isTrue();
    }

    @Then("the response carries a product holding id")
    public void theResponseCarriesAProductHoldingId() {
        String id = body().path("productHoldingId").asText();
        assertThat(id).isNotBlank();
        onboardedProductHoldingId = UUID.fromString(id);
    }

    @Then("the relationship and contact point reference the returned party")
    public void theRelationshipAndContactPointReferenceTheParty() {
        assertThat(scalar("SELECT count(*) FROM relationship WHERE party_id = ?", onboardedPartyId))
                .as("relationship rows pointing at the new party").isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM contact_point WHERE party_id = ?", onboardedPartyId))
                .as("contact point rows pointing at the new party").isEqualTo(1);
    }

    @Then("every {word} row is homed in region {string}")
    public void everyRowIsHomedInRegion(String table, String region) {
        long total = scalar("SELECT count(*) FROM " + table);
        long elsewhere = scalar(
                "SELECT count(*) FROM " + table + " WHERE crdb_region::string <> ?", region);
        assertThat(total).as("rows present in " + table).isGreaterThan(0);
        assertThat(elsewhere).as("rows of " + table + " homed outside " + region).isZero();
    }

    @Then("the voter replicas of the {word} table are pinned to region {string}")
    public void theVoterReplicasArePinnedToRegion(String table, String region) {
        if (!KNOWN_TABLES.contains(table)) {
            throw new IllegalArgumentException("Unknown table: " + table);
        }
        // This is the authoritative placement guarantee for a REGIONAL BY ROW table: CockroachDB
        // creates one partition per region and pins that partition's voting replicas and
        // leaseholder to the region. Range-level replica localities are NOT a valid check here,
        // because the table's key space is pre-split for every region, so empty foreign-region
        // ranges always appear.
        String pattern = "PARTITION " + region + " OF INDEX cdm.public." + table + "@%";
        List<String> configurations = strings(
                "SELECT raw_config_sql FROM [SHOW ZONE CONFIGURATIONS] WHERE target LIKE ?", pattern);

        assertThat(configurations)
                .as("zone configuration for the " + region + " partition of " + table)
                .isNotEmpty();
        assertThat(configurations).allSatisfy(configuration -> assertThat(configuration)
                .as("voting replicas of " + table + " must be pinned to " + region)
                .contains("voter_constraints = '[+region=" + region + "]'"));
    }

    @Then("the onboarded party is homed in region {string}")
    public void theOnboardedPartyIsHomedInRegion(String region) {
        assertThat(strings("SELECT crdb_region::string FROM party WHERE id = ?", onboardedPartyId))
                .containsExactly(region);
    }

    @Then("the onboarded product holding is homed in region {string}")
    public void theOnboardedProductHoldingIsHomedInRegion(String region) {
        assertThat(strings(
                "SELECT crdb_region::string FROM product_holding WHERE id = ?", onboardedProductHoldingId))
                .containsExactly(region);
    }

    @Then("the product holding pattern is {string}")
    public void theProductHoldingPatternIs(String expected) {
        assertThat(body().path("pattern").asText()).isEqualTo(expected);
    }

    @Then("the product holding region is {string}")
    public void theProductHoldingRegionIs(String expected) {
        assertThat(body().path("region").asText()).isEqualTo(expected);
    }

    @Then("the {word} row count is unchanged")
    public void theRowCountIsUnchanged(String table) {
        long before = rowCountsBefore.get(table);
        assertThat(scalar("SELECT count(*) FROM " + table))
                .as("row count of " + table + " after the failed operation")
                .isEqualTo(before);
    }

    @Then("the rollback demo reports rolled back is true")
    public void theRollbackDemoReportsRolledBack() {
        assertThat(body().path("rolledBack").asBoolean())
                .as("rollback demo body: " + response.getBody())
                .isTrue();
    }

    @Then("the rollback demo snapshots are identical")
    public void theRollbackDemoSnapshotsAreIdentical() {
        assertThat(body().path("before")).isEqualTo(body().path("after"));
    }

    @Then("both onboarding timings are recorded in the report")
    public void bothOnboardingTimingsAreRecorded() {
        assertThat(Files.exists(Path.of(METRICS_FILE))).isTrue();
        assertThat(readLines(METRICS_FILE)).hasSizeGreaterThanOrEqualTo(2);
    }

    @Then("posting to {string} returns {int}")
    public void postingToReturns(String path, int expectedStatus) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> attempt =
                rest.exchange(path, HttpMethod.POST, new HttpEntity<>("{}", headers), String.class);
        assertThat(attempt.getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @Then("the database has no table whose name contains {string} or {string}")
    public void theDatabaseHasNoTableContaining(String first, String second) {
        long matches = scalar(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' "
                        + "AND (table_name LIKE ? OR table_name LIKE ?)",
                "%" + first + "%", "%" + second + "%");
        assertThat(matches)
                .as("compensation bookkeeping tables matching " + first + " / " + second)
                .isZero();
    }

    @Then("the customer 360 read for the onboarded party is complete across all four domains")
    public void theCustomer360ReadIsComplete() {
        assertThat(scalar("SELECT count(*) FROM party WHERE id = ?", onboardedPartyId))
                .as("party rows for the onboarded party").isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM relationship WHERE party_id = ?", onboardedPartyId))
                .as("relationship rows for the onboarded party").isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM contact_point WHERE party_id = ?", onboardedPartyId))
                .as("contact point rows for the onboarded party").isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM product_holding WHERE party_id = ?", onboardedPartyId))
                .as("product holding rows for the onboarded party").isGreaterThanOrEqualTo(1);
    }

    @Then("the customer 360 read homes the party in {string} and the product holding in {string}")
    public void theCustomer360ReadHomesAcrossRegions(String partyRegion, String holdingRegion) {
        assertThat(strings("SELECT crdb_region::string FROM party WHERE id = ?", onboardedPartyId))
                .containsExactly(partyRegion);
        assertThat(strings(
                "SELECT crdb_region::string FROM product_holding WHERE id = ?", onboardedProductHoldingId))
                .containsExactly(holdingRegion);
    }

    // ------------------------------------- PoC B3: concurrency / serializability

    @When("I fire two concurrent cross-region onboardings for account {string}")
    public void iFireTwoConcurrentCrossRegionOnboardings(String accountNumber) {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<ResponseEntity<String>>> tasks = List.of(
                    () -> post("/api/onboarding/cross-region", onboardingPayload(
                            "Concurrent A Ltd", "HK_INSURANCE", accountNumber, "SAVINGS_ACCOUNT")),
                    () -> post("/api/onboarding/cross-region", onboardingPayload(
                            "Concurrent B Ltd", "HK_INSURANCE", accountNumber, "SAVINGS_ACCOUNT")));
            for (Future<ResponseEntity<String>> future : pool.invokeAll(tasks)) {
                concurrentStatuses.add(future.get().getStatusCode().value());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent onboarding was interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("Concurrent onboarding failed", e);
        } finally {
            pool.shutdown();
        }
        assertThat(concurrentStatuses).hasSize(2);
    }

    @Then("exactly one of the two onboardings succeeded")
    public void exactlyOneOfTheTwoOnboardingsSucceeded() {
        long succeeded = concurrentStatuses.stream().filter(status -> status == 200).count();
        assertThat(succeeded)
                .as("exactly one writer may win; observed statuses " + concurrentStatuses)
                .isEqualTo(1);
    }

    @Then("exactly {int} product holding exists for account {string}")
    public void exactlyOneProductHoldingExistsForAccount(int expected, String accountNumber) {
        assertThat(scalar("SELECT count(*) FROM product_holding WHERE account_number = ?",
                accountFor(accountNumber)))
                .as("committed product holdings for the contended account number")
                .isEqualTo(expected);
    }

    @Then("the party row count increased by exactly {int}")
    public void thePartyRowCountIncreasedBy(int delta) {
        assertThat(scalar("SELECT count(*) FROM party"))
                .as("the losing transaction must roll its UK party write back too")
                .isEqualTo(rowCountsBefore.get("party") + delta);
    }

    // ------------------------------------- PoC B4: region quorum loss

    @When("the Hong Kong region loses quorum")
    public void theHongKongRegionLosesQuorum() {
        List<String> hkNodes = CockroachClusterFixture.servicesStartingWith("hk");
        assertThat(hkNodes)
                .as("the fault scenario needs at least three HK nodes to lose write quorum")
                .hasSizeGreaterThanOrEqualTo(3);
        hkNodes.subList(1, hkNodes.size()).forEach(CockroachClusterFixture::stopService);
    }

    @When("I attempt a cross-region onboarding for account {string}")
    public void iAttemptACrossRegionOnboarding(String accountNumber) {
        partyCountBeforeFault = scalar("SELECT count(*) FROM party");
        response = post("/api/onboarding/cross-region",
                onboardingPayload("Quorum Ltd", "HK_INSURANCE", accountNumber, "SAVINGS_ACCOUNT"));
    }

    @Then("no party row was committed for that attempt")
    public void noPartyRowWasCommittedForThatAttempt() {
        assertThat(scalar("SELECT count(*) FROM party"))
                .as("a failed cross-region write must leave no orphaned UK party behind")
                .isEqualTo(partyCountBeforeFault);
    }

    @When("the cluster is serving reads again within {int} seconds")
    public void theClusterIsServingReadsAgainWithin(int seconds) {
        Instant deadline = Instant.now().plusSeconds(seconds);
        RuntimeException lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                scalar("SELECT count(*) FROM party");
                scalar("SELECT count(*) FROM product_holding");
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
            }
            pause(5000);
        }
        throw new IllegalStateException(
                "The cluster did not serve reads again within " + seconds + "s", lastFailure);
    }

    @When("the Hong Kong region is restored")
    public void theHongKongRegionIsRestored() {
        CockroachClusterFixture.servicesStartingWith("hk").forEach(CockroachClusterFixture::startService);
        pause(5000);
    }

    @Then("a later cross-region onboarding succeeds within {int} seconds")
    public void aLaterCrossRegionOnboardingSucceeds(int seconds) {
        Instant deadline = Instant.now().plusSeconds(seconds);
        int attempt = 0;
        while (Instant.now().isBefore(deadline)) {
            attempt++;
            ResponseEntity<String> attemptResponse = post("/api/onboarding/cross-region",
                    onboardingPayload("Recovery " + attempt + " Ltd", "HK_INSURANCE",
                            "HK-E2E-RECOVERY-" + attempt, "SAVINGS_ACCOUNT"));
            if (attemptResponse.getStatusCode().value() == 200) {
                return;
            }
            pause(5000);
        }
        throw new AssertionError("The region did not regain write quorum within " + seconds + "s");
    }

    // ------------------------------------- latency budget

    @Then("the cross-region onboarding is at least as slow as the region-local one")
    public void theCrossRegionOnboardingIsAtLeastAsSlow() {
        assertThat(ukOnboardingMillis).isGreaterThan(0);
        assertThat(crossRegionOnboardingMillis).isGreaterThan(0);
        if (Boolean.getBoolean("cdm.e2e.assertLatency")) {
            assertThat(crossRegionOnboardingMillis)
                    .as("a cross-region commit must cost at least the extra network hop")
                    .isGreaterThanOrEqualTo(ukOnboardingMillis);
        }
    }

    // ------------------------------------- safety net

    /** Always brings the HK nodes back so a failure in B4 cannot poison later scenarios. */
    @After
    public void restoreClusterAfterScenario() {
        try {
            CockroachClusterFixture.servicesStartingWith("hk").forEach(CockroachClusterFixture::startService);
        } catch (RuntimeException ignored) {
            // the cluster may already be gone (managed lifecycle); nothing to restore
        }
    }

    // ------------------------------------- PoC MIG: MongoDB to Distributed SQL

    @Given("the legacy store is available")
    public void theLegacyStoreIsAvailable() {
        Instant deadline = Instant.now().plusSeconds(60);
        int status = 0;
        while (Instant.now().isBefore(deadline)) {
            status = rest.getForEntity("/api/migration/gap", String.class).getStatusCode().value();
            if (status == 200) {
                return;
            }
            pause(2000);
        }
        throw new IllegalStateException(
                "The legacy MongoDB store was not available (last status " + status + ")");
    }

    @Given("the legacy store holds {int} migration customers")
    public void theLegacyStoreHoldsMigrationCustomers(int count) {
        for (int i = 1; i <= count; i++) {
            ResponseEntity<String> result = post("/api/migration/legacy-customers",
                    migrationCustomer(migrationName("MIG-Seed" + i)));
            assertThat(result.getStatusCode().value())
                    .as("seeding legacy customers: " + result.getBody())
                    .isEqualTo(201);
        }
    }

    @Given("the legacy store is fully reconciled")
    public void theLegacyStoreIsFullyReconciled() {
        post("/api/migration/backfill", Map.of());
        post("/api/migration/reconcile", Map.of());
    }

    @Given("I record the migration gap")
    public void iRecordTheMigrationGap() {
        migrationGapBefore = migrationGap();
    }

    @When("I run the migration backfill")
    public void iRunTheMigrationBackfill() {
        ResponseEntity<String> result = post("/api/migration/backfill", Map.of());
        assertThat(result.getStatusCode().value()).as("backfill: " + result.getBody()).isEqualTo(200);
    }

    @When("I run the migration reconciliation")
    public void iRunTheMigrationReconciliation() {
        ResponseEntity<String> result = post("/api/migration/reconcile", Map.of());
        assertThat(result.getStatusCode().value()).as("reconcile: " + result.getBody()).isEqualTo(200);
    }

    @When("I dual-write a migration customer named {string}")
    public void iDualWriteAMigrationCustomer(String name) {
        ResponseEntity<String> result = post("/api/migration/customers",
                migrationCustomer(migrationName(name)));
        assertThat(result.getStatusCode().value()).as("dual-write: " + result.getBody()).isEqualTo(200);
    }

    @When("the legacy system adds a migration customer named {string}")
    public void theLegacySystemAddsAMigrationCustomer(String name) {
        ResponseEntity<String> result = post("/api/migration/legacy-customers",
                migrationCustomer(migrationName(name)));
        assertThat(result.getStatusCode().value()).as("legacy write: " + result.getBody()).isEqualTo(201);
    }

    @Then("the migration gap is {int}")
    public void theMigrationGapIs(int expected) {
        assertThat(migrationGap()).as("migration gap").isEqualTo(expected);
    }

    @Then("the migration gap has increased by {int}")
    public void theMigrationGapHasIncreasedBy(int delta) {
        assertThat(migrationGap())
                .as("migration gap after a legacy-side change")
                .isEqualTo(migrationGapBefore + delta);
    }

    @Then("the CDM holds at least {int} migration parties")
    public void theCdmHoldsAtLeastMigrationParties(int expected) {
        assertThat(scalar("SELECT count(*) FROM party WHERE legal_name LIKE ?", "MIG-%"))
                .as("migration parties in the CDM")
                .isGreaterThanOrEqualTo(expected);
    }

    @Then("the CDM holds a migration party named {string}")
    public void theCdmHoldsAMigrationPartyNamed(String name) {
        assertThat(scalar("SELECT count(*) FROM party WHERE legal_name = ?", migrationName(name)))
                .as("CDM counterpart for " + name)
                .isEqualTo(1);
    }

    @Then("the CDM holds exactly {int} migration party named {string}")
    public void theCdmHoldsExactlyMigrationPartiesNamed(int expected, String name) {
        assertThat(scalar("SELECT count(*) FROM party WHERE legal_name = ?", migrationName(name)))
                .as("idempotency of backfill and reconciliation for " + name)
                .isEqualTo(expected);
    }

    // ------------------------------------- PoC RM: eventually consistent read model

    @When("I run the read-model projection")
    public void iRunTheReadModelProjection() {
        ResponseEntity<String> result = post("/api/readmodel/project", Map.of());
        assertThat(result.getStatusCode().value()).as("projection: " + result.getBody()).isEqualTo(200);
    }

    @When("I open a product holding {string} numbered {string} for the onboarded party")
    public void iOpenAProductHoldingForTheOnboardedParty(String productType, String accountNumber) {
        ResponseEntity<String> result = post("/api/product-holdings",
                productHoldingPayload(onboardedPartyId, productType, accountNumber));
        assertThat(result.getStatusCode().value())
                .as("extra holding: " + result.getBody())
                .isEqualTo(200);
    }

    @Then("the customer-360 read for the onboarded party is not available yet")
    public void theCustomer360ReadIsNotAvailableYet() {
        ResponseEntity<String> result = rest.getForEntity(
                "/api/customer-360/" + onboardedPartyId, String.class);
        assertThat(result.getStatusCode().value())
                .as("the write path must not maintain the projection")
                .isEqualTo(404);
    }

    @Then("the customer-360 read for the onboarded party is complete")
    public void theProjectedCustomer360ReadIsComplete() {
        JsonNode view = customer360View();
        assertThat(view.path("legalName").asText()).isNotBlank();
        assertThat(view.path("market").asText()).isNotBlank();
        assertThat(view.path("contactPoint").asText()).isNotBlank();
        assertThat(view.path("accountNumbers").asText()).isNotBlank();
    }

    @Then("the customer-360 read for the onboarded party lists {int} account numbers")
    public void theCustomer360ReadListsAccountNumbers(int expected) {
        String accounts = customer360View().path("accountNumbers").asText();
        assertThat(accounts.split(",")).as("projected accounts: " + accounts).hasSize(expected);
    }

    // ------------------------------------------------------------------ helpers

    private ResponseEntity<String> post(String path, Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
    }

    private JsonNode body() {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Response body was not JSON: " + response.getBody(), e);
        }
    }

    private void captureOnboardingResult() {
        if (!response.getStatusCode().is2xxSuccessful()) {
            return;
        }
        JsonNode json = body();
        onboardedPartyId = uuidOrNull(json.path("partyId"));
        onboardedProductHoldingId = uuidOrNull(json.path("productHoldingId"));
    }

    private static UUID uuidOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() || node.asText().isBlank()
                ? null
                : UUID.fromString(node.asText());
    }

    private static Map<String, Object> onboardingPayload(
            String legalName, String market, String accountNumber, String productType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("legalName", legalName);
        payload.put("partyType", "ORGANIZATION");
        payload.put("market", market);
        payload.put("lineOfBusiness", "E2E");
        payload.put("contactPointType", "EMAIL");
        payload.put("contactPointValue", "e2e@example.test");
        payload.put("productType", productType);
        payload.put("accountNumber", accountNumber == null ? null : accountFor(accountNumber));
        return payload;
    }

    private static Map<String, Object> productHoldingPayload(
            UUID owner, String productType, String accountNumber) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partyId", owner.toString());
        payload.put("productType", productType);
        payload.put("accountNumber", accountFor(accountNumber));
        payload.put("market", "HK_INSURANCE");
        return payload;
    }

    /** Run-scoped migration customer name, so repeated runs never collide. */
    private static String migrationName(String base) {
        return base + "-" + RUN_ID;
    }

    /** A legacy document shaped exactly as the legacy system stores it. */
    private static Map<String, Object> migrationCustomer(String legalName) {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("legalName", legalName);
        customer.put("partyType", "ORGANIZATION");
        customer.put("market", "UK_RETAIL");
        customer.put("lineOfBusiness", "RETAIL");
        customer.put("contactPointType", "EMAIL");
        customer.put("contactPointValue",
                legalName.toLowerCase().replaceAll("[^a-z0-9]", "") + "@legacy.test");
        customer.put("productType", "SAVINGS_ACCOUNT");
        customer.put("accountNumber", "LEG-" + legalName);
        return customer;
    }

    private long migrationGap() {
        ResponseEntity<String> result = rest.getForEntity("/api/migration/gap", String.class);
        assertThat(result.getStatusCode().value())
                .as("gap response: " + result.getBody())
                .isEqualTo(200);
        try {
            return objectMapper.readTree(result.getBody()).path("gap").asLong();
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the migration gap: " + result.getBody(), e);
        }
    }

    /** Single source of truth for the run-scoped account number every scenario uses. */
    private static String accountFor(String accountNumber) {
        return accountNumber + "-" + RUN_ID;
    }

    /** Reads the 360 view from the read-model projection API. */
    private JsonNode customer360View() {
        ResponseEntity<String> result = rest.getForEntity(
                "/api/customer-360/" + onboardedPartyId, String.class);
        assertThat(result.getStatusCode().value())
                .as("360 view: " + result.getBody())
                .isEqualTo(200);
        try {
            return objectMapper.readTree(result.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the 360 view: " + result.getBody(), e);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting", e);
        }
    }

    private long scalar(String sql, Object... args) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).as("query returned a row: " + sql).isTrue();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + sql, e);
        }
    }

    private List<String> strings(String sql, Object... args) {
        List<String> values = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    values.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + sql, e);
        }
        return values;
    }

    private static void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
        }
    }

    private static List<String> readLines(String path) {
        try {
            return Files.readAllLines(Path.of(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }

    /**
     * Records a latency sample so the per-PoC report can show the measured cost of Pattern A
     * against Pattern B.
     *
     * <p>On a single host the four nodes share one kernel, so there is no real inter-region network
     * hop and a "B is slower than A" assertion would be meaningless. The numbers are therefore
     * recorded as evidence rather than asserted as a threshold; a true latency budget must be
     * measured on genuinely separated regions.
     */
    private static void recordLatency(String pattern, long elapsedMillis) {
        try {
            Path file = Path.of(METRICS_FILE);
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, "pattern,elapsed_millis\n", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE);
            }
            Files.writeString(file, pattern + "," + elapsedMillis + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            throw new IllegalStateException("Could not record latency metric", e);
        }
    }
}
