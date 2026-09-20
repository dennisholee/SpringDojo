package io.forest.cdm.party;

import io.forest.cdm.shared.CdmDomainException;

import java.util.UUID;

/**
 * Thrown when an operation references a party that does not exist.
 *
 * <p>Part of the {@code party} module's public API so that dependent modules (for example
 * {@code productholding}) can enforce referential integrity inside their own transactions.
 */
public class PartyNotFoundException extends CdmDomainException {

    public PartyNotFoundException(UUID partyId) {
        super("PARTY_NOT_FOUND", "Party not found: " + partyId);
    }
}
