package io.forest.cdm.party.internal;

import io.forest.cdm.party.PartyRow;
import io.forest.cdm.shared.Region;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound port for party persistence, owned by the {@code party} module.
 *
 * <p>It lives in {@code internal} because it is only ever consumed inside this module: a port is
 * part of the module's design, not of its published API. The driven adapter
 * ({@link JdbcPartyRepository}) implements it, which is what allows the domain side to be tested
 * against a hand-written fake instead of a mocked framework.
 *
 * <p>The adapter is responsible for pinning rows to the region the party is mastered in; that
 * placement rule is the residency contract and must not leak to callers.
 */
public interface PartyRepository {

    void insert(UUID id, String partyType, String legalName, Region masterRegion);

    boolean exists(UUID id);

    boolean existsByLegalName(String legalName);

    long countByLegalNamePrefix(String prefix);

    List<PartyRow> listParties(int limit);

    Map<Region, Long> countsByRegion();
}
