package io.forest.cdm.party;

import java.util.UUID;

/** Minimal party projection used by the read-model projector. */
public record PartyRow(UUID id, String legalName) {
}
