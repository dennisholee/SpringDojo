package io.forest.cdm.contactpoint;

import io.forest.cdm.contactpoint.internal.ContactPointRepository;
import io.forest.cdm.shared.Region;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Public API of the {@code contactpoint} module (UK Core Domain).
 *
 * <p>Like Party, contact points are part of the UK customer master and are therefore written
 * into the {@code uk} region.
 */
@Service
public class ContactPointService {

    private final ContactPointRepository repository;

    public ContactPointService(ContactPointRepository repository) {
        this.repository = repository;
    }

    public UUID createContactPoint(UUID partyId, ContactPointType type, String value) {
        UUID id = UUID.randomUUID();
        repository.insert(id, partyId, type.name(), value);
        return id;
    }

    /** Read-model support: the contact points that belong to a party. */
    public java.util.List<ContactPointRow> findForParty(java.util.UUID partyId) {
        return repository.findForParty(partyId);
    }

    /** Residency proof helper: how many contact point rows physically live in each region. */
    public Map<Region, Long> rowCountsByRegion() {
        return repository.countsByRegion();
    }
}
