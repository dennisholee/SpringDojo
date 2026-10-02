package io.forest.cdm.readmodel;

import java.util.UUID;

/**
 * The denormalised customer-360 row served by the read model.
 *
 * <p>Deliberately flat: it is a query-side artefact, not a domain entity. It is written only by the
 * projector and read only through {@code GET /api/customer-360/{partyId}}.
 */
public record Customer360View(
        UUID partyId,
        String legalName,
        String market,
        String lineOfBusiness,
        String contactPoint,
        String accountNumbers
) {
}
