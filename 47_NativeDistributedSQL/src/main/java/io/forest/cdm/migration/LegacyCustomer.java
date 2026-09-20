package io.forest.cdm.migration;

/**
 * A customer as the legacy system stores it: one MongoDB document holding the party, its
 * relationship, its contact point and its product holding.
 *
 * <p>The migration translates this shape into the normalised Distributed SQL tables owned by the
 * {@code party}, {@code relationship}, {@code contactpoint} and {@code productholding} modules.
 */
public record LegacyCustomer(
        String legalName,
        String partyType,
        String market,
        String lineOfBusiness,
        String contactPointType,
        String contactPointValue,
        String productType,
        String accountNumber
) {
}
