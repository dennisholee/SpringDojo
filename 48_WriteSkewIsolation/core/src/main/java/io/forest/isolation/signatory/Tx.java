package io.forest.isolation.signatory;

/**
 * The per-transaction read/write surface the invariant is expressed against. Both adapters must
 * implement it, so the harness can drive either engine without knowing which one it holds.
 */
public interface Tx {

    /** A consistent read of how many signatories are currently authorized for the party. */
    int countAuthorized(String partyId);

    /** Writes exactly one row/document: the signatory's own authorization flag. */
    void setAuthorized(String partyId, String signatoryId, boolean authorized);

    /**
     * Writes the party's shared counter row. Under snapshot isolation two transactions that call
     * this race on the same row, which turns the otherwise invisible read-write dependency into a
     * detectable write-write conflict. Strict serializable engines do not need it.
     */
    void touch(String partyId);
}
