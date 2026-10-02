package io.forest.cdm.migration.internal;

import io.forest.cdm.contactpoint.ContactPointService;
import io.forest.cdm.contactpoint.ContactPointType;
import io.forest.cdm.migration.LegacyCustomer;
import io.forest.cdm.party.PartyService;
import io.forest.cdm.party.PartyType;
import io.forest.cdm.productholding.ProductHoldingService;
import io.forest.cdm.relationship.RelationshipService;
import io.forest.cdm.shared.Region;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Translates one legacy document into the normalised CDM tables, in a single transaction.
 *
 * <p>Kept separate from {@link MigrationService} on purpose: the sweep reads many legacy documents
 * from MongoDB and must not hold a SQL transaction open while doing so, but each customer's write
 * still needs to be atomic. A separate bean gives each call its own proxied transaction.
 */
@Service
public class MigrationWriter {

    private final PartyService partyService;
    private final RelationshipService relationshipService;
    private final ContactPointService contactPointService;
    private final ProductHoldingService productHoldingService;

    public MigrationWriter(PartyService partyService,
                           RelationshipService relationshipService,
                           ContactPointService contactPointService,
                           ProductHoldingService productHoldingService) {
        this.partyService = partyService;
        this.relationshipService = relationshipService;
        this.contactPointService = contactPointService;
        this.productHoldingService = productHoldingService;
    }

    @Transactional
    public UUID write(LegacyCustomer customer) {
        UUID partyId = partyService.createParty(
                PartyType.valueOf(customer.partyType()), customer.legalName(), Region.UK);
        relationshipService.createRelationship(
                partyId, customer.market(), customer.lineOfBusiness());
        contactPointService.createContactPoint(
                partyId,
                ContactPointType.valueOf(customer.contactPointType()),
                customer.contactPointValue());

        if (customer.accountNumber() != null && !customer.accountNumber().isBlank()) {
            productHoldingService.createProductHolding(
                    partyId, customer.productType(), customer.accountNumber(), customer.market());
        }
        return partyId;
    }
}
