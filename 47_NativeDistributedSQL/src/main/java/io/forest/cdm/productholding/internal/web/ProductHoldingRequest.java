package io.forest.cdm.productholding.internal.web;

import java.util.UUID;

/**
 * Request to open a product holding (for example an account) in the HK market region.
 *
 * @param partyId must reference an existing party; referential integrity is enforced inside the
 *                same transaction
 */
public record ProductHoldingRequest(
        UUID partyId,
        String productType,
        String accountNumber,
        String market
) {
}
