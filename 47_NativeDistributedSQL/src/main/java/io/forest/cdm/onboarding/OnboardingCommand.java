package io.forest.cdm.onboarding;

import io.forest.cdm.contactpoint.ContactPointType;
import io.forest.cdm.party.PartyType;

/**
 * Input for both the region-local and the cross-region onboarding flows.
 *
 * <p>Note the deliberate asymmetry: the customer is always a UK-mastered Party, while the
 * product holding is an HK-issued account. The same request therefore drives either a
 * region-local (A) or a cross-region (B) transaction purely by which modules it fans out to.
 */
public record OnboardingCommand(
        String legalName,
        PartyType partyType,
        String market,
        String lineOfBusiness,
        ContactPointType contactPointType,
        String contactPointValue,
        String productType,
        String accountNumber
) {
}
