package io.forest.isolation.signatory;

import java.util.List;

/**
 * Everything a scenario needs to assert on after one concurrent-resignation run: whether each
 * transaction committed, whether each resignation actually happened, the failure (if any), the
 * authoritative end state, and the ordered per-transaction evidence trail.
 */
public record Outcome(
        boolean aliceCommitted,
        boolean bobCommitted,
        boolean aliceRevoked,
        boolean bobRevoked,
        Throwable aliceError,
        Throwable bobError,
        int finalAuthorized,
        List<String> trace) {

    /** How many of the two transactions committed. */
    public int commits() {
        return (aliceCommitted ? 1 : 0) + (bobCommitted ? 1 : 0);
    }

    /** How many of the two resignations actually took effect. */
    public int resignations() {
        return (aliceRevoked ? 1 : 0) + (bobRevoked ? 1 : 0);
    }

    /** The first side's failure, if either side failed. */
    public Throwable failure() {
        return aliceError != null ? aliceError : bobError;
    }
}
