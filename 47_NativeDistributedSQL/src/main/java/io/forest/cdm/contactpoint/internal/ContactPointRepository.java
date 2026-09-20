package io.forest.cdm.contactpoint.internal;

import io.forest.cdm.contactpoint.ContactPointRow;
import io.forest.cdm.shared.Region;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound port for contact point persistence, owned by the {@code contactpoint} module and
 * implemented by {@link JdbcContactPointRepository}.
 */
public interface ContactPointRepository {

    void insert(UUID id, UUID partyId, String contactType, String contactValue);

    List<ContactPointRow> findForParty(UUID partyId);

    Map<Region, Long> countsByRegion();
}
