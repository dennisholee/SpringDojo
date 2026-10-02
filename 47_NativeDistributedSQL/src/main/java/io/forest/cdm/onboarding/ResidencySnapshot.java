package io.forest.cdm.onboarding;

import java.util.Map;

/**
 * Row counts per region, per table.
 *
 * <p>Used as the data-residency proof: after any number of operations, {@code party},
 * {@code relationship} and {@code contactPoint} rows must only ever appear under {@code uk},
 * while {@code productHolding} rows must only appear under {@code hk}. Anything else means the
 * placement policy is wrong.
 */
public record ResidencySnapshot(
        Map<String, Long> party,
        Map<String, Long> relationship,
        Map<String, Long> contactPoint,
        Map<String, Long> productHolding
) {
}
