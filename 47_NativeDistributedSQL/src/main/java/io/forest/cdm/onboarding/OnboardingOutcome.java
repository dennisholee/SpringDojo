package io.forest.cdm.onboarding;

import java.util.UUID;

/**
 * Identifiers produced by an onboarding flow.
 *
 * @param pattern {@code A_REGION_LOCAL} or {@code B_CROSS_REGION}
 * @param productHoldingId {@code null} for the UK-only (Pattern A) flow
 */
public record OnboardingOutcome(
        UUID partyId,
        UUID relationshipId,
        UUID contactPointId,
        UUID productHoldingId,
        String pattern
) {
}
