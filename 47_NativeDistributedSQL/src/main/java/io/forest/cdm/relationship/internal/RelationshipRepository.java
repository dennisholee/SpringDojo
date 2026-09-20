package io.forest.cdm.relationship.internal;

import io.forest.cdm.relationship.RelationshipRow;
import io.forest.cdm.shared.Region;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound port for relationship persistence, owned by the {@code relationship} module and
 * implemented by {@link JdbcRelationshipRepository}.
 */
public interface RelationshipRepository {

    void insert(UUID id, UUID partyId, String market, String lineOfBusiness);

    List<RelationshipRow> findForParty(UUID partyId);

    Map<Region, Long> countsByRegion();
}
