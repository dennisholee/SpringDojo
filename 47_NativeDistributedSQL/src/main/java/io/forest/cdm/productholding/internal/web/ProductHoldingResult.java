package io.forest.cdm.productholding.internal.web;

import java.util.UUID;

/**
 * Result of opening a product holding.
 *
 * @param region the region the row was pinned to, so the caller can assert residency
 * @param pattern always {@code A_REGION_LOCAL} because a product holding write touches one region
 */
public record ProductHoldingResult(
        UUID productHoldingId,
        String region,
        String pattern
) {
}
