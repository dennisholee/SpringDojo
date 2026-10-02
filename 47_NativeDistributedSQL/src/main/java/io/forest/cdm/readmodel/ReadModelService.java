package io.forest.cdm.readmodel;

import io.forest.cdm.contactpoint.ContactPointRow;
import io.forest.cdm.contactpoint.ContactPointService;
import io.forest.cdm.party.PartyRow;
import io.forest.cdm.party.PartyService;
import io.forest.cdm.productholding.ProductHoldingRow;
import io.forest.cdm.productholding.ProductHoldingService;
import io.forest.cdm.readmodel.internal.ProjectionRepository;
import io.forest.cdm.relationship.RelationshipRow;
import io.forest.cdm.relationship.RelationshipService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pattern C: a denormalised customer-360 projection, maintained <b>outside</b> the write path.
 *
 * <p>The writers never touch {@code customer_360}: that is the whole point. The projection is
 * converged separately, so a cross-region read never has to join four tables across two regions and
 * never has to pay for cross-region ACID.
 *
 * <p>The catch-up pass below is a sweep. Its production form is a CockroachDB changefeed consumer,
 * which is a deployment change rather than a change to this contract: the guarantee being tested is
 * eventual convergence of the projection, decoupled from the transaction that made the write.
 */
@Service
public class ReadModelService {

    private static final int PROJECTION_LIMIT = 500;

    private final PartyService partyService;
    private final RelationshipService relationshipService;
    private final ContactPointService contactPointService;
    private final ProductHoldingService productHoldingService;
    private final ProjectionRepository projection;

    private volatile String lastRun = "never";

    public ReadModelService(PartyService partyService,
                            RelationshipService relationshipService,
                            ContactPointService contactPointService,
                            ProductHoldingService productHoldingService,
                            ProjectionRepository projection) {
        this.partyService = partyService;
        this.relationshipService = relationshipService;
        this.contactPointService = contactPointService;
        this.productHoldingService = productHoldingService;
        this.projection = projection;
    }

    /**
     * One projection pass: rebuilds the 360 row for every party.
     *
     * @return how many denormalised rows were written
     */
    public int project() {
        int projected = 0;
        for (PartyRow party : partyService.listParties(PROJECTION_LIMIT)) {
            projection.upsert(build(party));
            projected++;
        }
        lastRun = Instant.now().toString();
        return projected;
    }

    /** Serves the 360 view from the projection only - never from the source tables. */
    public Optional<Customer360View> read(UUID partyId) {
        return projection.find(partyId);
    }

    public long projectionCount() {
        return projection.count();
    }

    public String lastRun() {
        return lastRun;
    }

    private Customer360View build(PartyRow party) {
        List<RelationshipRow> relationships = relationshipService.findForParty(party.id());
        List<ContactPointRow> contacts = contactPointService.findForParty(party.id());
        List<ProductHoldingRow> holdings = productHoldingService.findForParty(party.id());

        return new Customer360View(
                party.id(),
                party.legalName(),
                relationships.isEmpty() ? null : relationships.get(0).market(),
                relationships.isEmpty() ? null : relationships.get(0).lineOfBusiness(),
                contacts.isEmpty() ? null : contacts.get(0).value(),
                holdings.stream().map(ProductHoldingRow::accountNumber).collect(Collectors.joining(",")));
    }
}
