package io.forest.cdm.productholding;

/** Minimal product holding projection used by the read-model projector. */
public record ProductHoldingRow(String accountNumber, String market) {
}
