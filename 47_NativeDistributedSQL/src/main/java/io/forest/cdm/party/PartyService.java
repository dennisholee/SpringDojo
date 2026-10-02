package io.forest.cdm.party;

import io.forest.cdm.party.internal.PartyRepository;
import io.forest.cdm.shared.Region;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Public API of the {@code party} module (UK Core Domain).
 *
 * <p>This is the only type other modules may touch. The persistence details live in the
 * module-internal {@link PartyRepository} and are deliberately not reachable from outside.
 *
 * <p>Party is a UK-mastered golden record, so every row is written into the {@code uk}
 * region regardless of the caller's location.
 */
@Service
public class PartyService {

    private final PartyRepository repository;

    public PartyService(PartyRepository repository) {
        this.repository = repository;
    }

    /**
     * @param masterRegion region in which the party is treated as the master record
     * @return the identifier of the newly created party
     */
    public UUID createParty(PartyType partyType, String legalName, Region masterRegion) {
        UUID id = UUID.randomUUID();
        repository.insert(id, partyType.name(), legalName, masterRegion);
        return id;
    }

    /**
     * Enforces referential integrity for dependent modules.
     *
     * <p>Called inside the caller's transaction, so a missing party aborts the whole unit of work
     * rather than leaving a dangling reference behind.
     *
     * @throws PartyNotFoundException if no party exists with the supplied identifier
     */
    public void requirePartyExists(UUID partyId) {
        if (!repository.exists(partyId)) {
            throw new PartyNotFoundException(partyId);
        }
    }

    /**
     * Used by the migration reconciler to decide whether a legacy customer already has a
     * counterpart in the CDM store. The legal name is the natural key between the two stores.
     */
    public boolean existsByLegalName(String legalName) {
        return repository.existsByLegalName(legalName);
    }

    /** Used by the migration reconciler to compute the remaining gap. */
    public long countByLegalNamePrefix(String prefix) {
        return repository.countByLegalNamePrefix(prefix);
    }

    /** Read-model support: the parties a 360 projection is built from. */
    public java.util.List<PartyRow> listParties(int limit) {
        return repository.listParties(limit);
    }

    /** Residency proof helper: how many party rows physically live in each region. */
    public Map<Region, Long> rowCountsByRegion() {
        return repository.countsByRegion();
    }
}
