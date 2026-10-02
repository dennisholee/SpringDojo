package io.forest.cdm.relationship;

import io.forest.cdm.relationship.internal.RelationshipRepository;
import io.forest.cdm.shared.Region;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Public API of the {@code relationship} module (UK Core Domain).
 *
 * <p>A relationship links a party to a market and line of business (e.g. {@code UK_RETAIL},
 * {@code HK_INSURANCE}). It is mastered in the UK, even when it describes an HK market - this is
 * precisely why onboarding an HK customer touches both regions.
 */
@Service
public class RelationshipService {

    private final RelationshipRepository repository;

    public RelationshipService(RelationshipRepository repository) {
        this.repository = repository;
    }

    public UUID createRelationship(UUID partyId, String market, String lineOfBusiness) {
        UUID id = UUID.randomUUID();
        repository.insert(id, partyId, market, lineOfBusiness);
        return id;
    }

    /** Read-model support: the relationships that belong to a party. */
    public java.util.List<RelationshipRow> findForParty(java.util.UUID partyId) {
        return repository.findForParty(partyId);
    }

    /** Residency proof helper: how many relationship rows physically live in each region. */
    public Map<Region, Long> rowCountsByRegion() {
        return repository.countsByRegion();
    }
}
