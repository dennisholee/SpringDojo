package io.forest.isolation.signatory;

/**
 * The business rule, expressed once and engine-agnostic: a party (customer) must always keep at
 * least one authorized signatory. It is a count/absence constraint, so no unique index can protect
 * it — which is exactly why snapshot isolation permits write skew here.
 */
public final class SignatoryGuard {

    public static final int MIN_AUTHORIZED = 1;

    private SignatoryGuard() {
    }

    /** Revokes a signatory unless doing so would leave the party with none. */
    public static boolean revoke(Tx tx, String partyId, String signatoryId) {
        if (tx.countAuthorized(partyId) <= MIN_AUTHORIZED) {
            return false;         // refuse: would leave the party with no authorized signatory
        }
        tx.setAuthorized(partyId, signatoryId, false);
        return true;
    }

    /**
     * The same rule, preceded by a write to the party's shared counter row. This is the
     * application-level mitigation snapshot isolation requires: it reintroduces a serialization
     * point so that two concurrent revocations cannot both pass the count check.
     */
    public static boolean revokeGuarded(Tx tx, String partyId, String signatoryId) {
        tx.touch(partyId);
        return revoke(tx, partyId, signatoryId);
    }
}
