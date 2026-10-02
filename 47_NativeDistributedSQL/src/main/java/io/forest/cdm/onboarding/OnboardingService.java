package io.forest.cdm.onboarding;

import io.forest.cdm.contactpoint.ContactPointService;
import io.forest.cdm.party.PartyService;
import io.forest.cdm.productholding.ProductHoldingService;
import io.forest.cdm.relationship.RelationshipService;
import io.forest.cdm.shared.Region;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The composite CDM use case.
 *
 * <p>This module is the replacement for the old "process layer": instead of orchestrating remote
 * system services over HTTP and then compensating with Saga/TCC, it calls the owning modules
 * <em>in-process</em>. Every method is a single SQL transaction on the shared distributed SQL
 * database, so atomicity and rollback are the database's job.
 *
 * <ul>
 *     <li><b>Pattern A</b> ({@link #onboardUkLocal}) - every row lands in {@code uk}; a
 *     region-local transaction with local commit latency.</li>
 *     <li><b>Pattern B</b> ({@link #onboardCrossRegion}) - {@code party}/{@code relationship}/
 *     {@code contact_point} in {@code uk} plus {@code product_holding} in {@code hk}; a genuine
 *     cross-region ACID transaction with a measurable commit-latency penalty.</li>
 * </ul>
 */
@Service
public class OnboardingService {

    private final PartyService partyService;
    private final RelationshipService relationshipService;
    private final ContactPointService contactPointService;
    private final ProductHoldingService productHoldingService;

    public OnboardingService(PartyService partyService,
                             RelationshipService relationshipService,
                             ContactPointService contactPointService,
                             ProductHoldingService productHoldingService) {
        this.partyService = partyService;
        this.relationshipService = relationshipService;
        this.contactPointService = contactPointService;
        this.productHoldingService = productHoldingService;
    }

    /**
     * Pattern A: UK-only onboarding.
     *
     * <p>All three writes are homed in the {@code uk} region, so the transaction never leaves the
     * region on its commit path. No product holding is created.
     */
    @Transactional
    public OnboardingOutcome onboardUkLocal(OnboardingCommand request) {
        UUID partyId = partyService.createParty(request.partyType(), request.legalName(), Region.UK);
        UUID relationshipId = relationshipService.createRelationship(
                partyId, request.market(), request.lineOfBusiness());
        UUID contactPointId = contactPointService.createContactPoint(
                partyId, request.contactPointType(), request.contactPointValue());
        return new OnboardingOutcome(partyId, relationshipId, contactPointId, null, "A_REGION_LOCAL");
    }

    /**
     * Pattern B: cross-region onboarding.
     *
     * <p>Adds an HK-issued product holding to the UK-mastered customer in the <em>same</em>
     * transaction. The engine coordinates the two regions under strict serializable isolation;
     * there is no compensation handler anywhere in this class.
     */
    @Transactional
    public OnboardingOutcome onboardCrossRegion(OnboardingCommand request) {
        OnboardingOutcome uk = onboardUkLocal(request);
        UUID productHoldingId = productHoldingService.createProductHolding(
                uk.partyId(), request.productType(), request.accountNumber(), request.market());
        return new OnboardingOutcome(
                uk.partyId(), uk.relationshipId(), uk.contactPointId(), productHoldingId, "B_CROSS_REGION");
    }

    /**
     * Pattern B negative test: writes UK and HK rows, then fails.
     *
     * <p>Demonstrates that a mid-flight failure after the cross-region writes propagates as a
     * transaction abort. Nothing is committed, so there is no "orphaned party" state to
     * compensate for.
     */
    @Transactional
    public void onboardCrossRegionWithFailure(OnboardingCommand request) {
        onboardCrossRegion(request);
        throw new IllegalStateException(
                "Simulated downstream failure after cross-region writes; expect a full rollback");
    }

    /** Aggregates the per-module residency counts into a single snapshot. */
    public ResidencySnapshot residencySnapshot() {
        return new ResidencySnapshot(
                toRegionNameMap(partyService.rowCountsByRegion()),
                toRegionNameMap(relationshipService.rowCountsByRegion()),
                toRegionNameMap(contactPointService.rowCountsByRegion()),
                toRegionNameMap(productHoldingService.rowCountsByRegion()));
    }

    private static Map<String, Long> toRegionNameMap(Map<Region, Long> counts) {
        Map<String, Long> byName = new LinkedHashMap<>();
        for (Region region : Region.values()) {
            byName.put(region.sqlName(), counts.getOrDefault(region, 0L));
        }
        return byName;
    }
}
