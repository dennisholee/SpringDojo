package io.forest.isolation.signatory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Forces the one interleaving that exhibits write skew (read-read, then write-write) with a latch,
 * so the result is reproducible instead of a flaky race.
 *
 * <p>Whichever engine it is handed, the harness only talks to the {@link SignatoryStore} port; the
 * isolation level of that engine decides the verdict.
 */
public final class ConcurrentRevoke {

    private static final String PARTY_ID = "P1";
    private static final String ALICE = "alice";
    private static final String BOB = "bob";

    private ConcurrentRevoke() {
    }

    /**
     * Both parties resign concurrently. When {@code retryOnSerialization} is set, a transaction
     * aborted with {@code SQLSTATE 40001} is retried once — the application pattern strict
     * serializable engines require and snapshot isolation lets you skip.
     */
    public static Outcome run(SignatoryStore store, boolean retryOnSerialization) {
        return run(store, retryOnSerialization, false);
    }

    /**
     * The application-level mitigation: both transactions write the party's shared counter row, so
     * a snapshot-isolated engine detects a write-write conflict and aborts one of them.
     */
    public static Outcome runGuarded(SignatoryStore store) {
        return run(store, false, true);
    }

    private static Outcome run(SignatoryStore store, boolean retry, boolean guarded) {
        CountDownLatch bothRead = new CountDownLatch(2);
        Attempt alice = new Attempt();
        Attempt bob = new Attempt();
        List<String> trace = new CopyOnWriteArrayList<>();

        trace.add("mode=" + (guarded ? "guarded" : retry ? "retry-on-serialization" : "plain")
                + " interleaving=read-read-then-write-write (latch-forced)");

        Thread first = new Thread(() -> attempt(store, ALICE, bothRead, retry, guarded, alice, trace),
                "tx-alice");
        Thread second = new Thread(() -> attempt(store, BOB, bothRead, retry, guarded, bob, trace),
                "tx-bob");
        first.start();
        second.start();
        join(first);
        join(second);

        int finalAuthorized = store.inTransaction(tx -> tx.countAuthorized(PARTY_ID));
        trace.add("harness | VERIFY party=" + PARTY_ID + " authorized=" + finalAuthorized
                + " (minimum required=" + SignatoryGuard.MIN_AUTHORIZED + ")");
        return new Outcome(alice.committed, bob.committed, alice.revoked, bob.revoked,
                alice.error, bob.error, finalAuthorized, List.copyOf(trace));
    }

    private static void attempt(SignatoryStore store, String signatoryId, CountDownLatch bothRead,
                                boolean retry, boolean guarded, Attempt out, List<String> trace) {
        String who = Thread.currentThread().getName();
        try {
            out.revoked = store.inTransaction(tx -> {
                int authorized = tx.countAuthorized(PARTY_ID);   // the READ happens before either write
                trace.add(who + " | READ   party=" + PARTY_ID + " -> " + authorized + " authorized");
                awaitBarrier(bothRead);
                boolean revoked = guarded
                        ? SignatoryGuard.revokeGuarded(tx, PARTY_ID, signatoryId)
                        : SignatoryGuard.revoke(tx, PARTY_ID, signatoryId);
                trace.add(who + " | WRITE  signatory=" + signatoryId + " authorized=false"
                        + (revoked ? "" : " [refused: would break the invariant]"));
                return revoked;
            });
            out.committed = true;
            trace.add(who + " | COMMIT revoked=" + out.revoked);
        } catch (RuntimeException e) {
            trace.add(who + " | ABORT  " + describe(e));
            if (retry && isSerializationFailure(e)) {
                out.revoked = store.inTransaction(tx -> SignatoryGuard.revoke(tx, PARTY_ID, signatoryId));
                trace.add(who + " | RETRY  after SQLSTATE 40001 -> revoked=" + out.revoked);
            }
            out.error = e;
        }
    }

    /** Makes both threads read before either writes. */
    private static void awaitBarrier(CountDownLatch bothRead) {
        bothRead.countDown();
        try {
            bothRead.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while forcing the interleaving", e);
        }
    }

    /** True when the failure chain contains a CockroachDB serialization failure (SQLSTATE 40001). */
    static boolean isSerializationFailure(Throwable error) {
        return "40001".equals(sqlState(error));
    }

    /** The SQLSTATE of the first {@link SQLException} in the failure chain, or {@code null}. */
    static String sqlState(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
    }

    /**
     * A one-line, evidence-grade reason for a transaction failure. CockroachDB supplies a SQLSTATE
     * ({@code 40001}); MongoDB supplies a transient write-conflict error instead.
     */
    static String describe(Throwable error) {
        Throwable root = rootCause(error);
        String reason = root.getClass().getSimpleName() + ": " + oneLine(root.getMessage());
        String state = sqlState(error);
        return state == null ? reason : "SQLSTATE=" + state + " (" + reason + ")";
    }

    private static Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static String oneLine(String message) {
        if (message == null) {
            return "";
        }
        String first = message.strip().split("\\R", 2)[0];
        return first.length() > 140 ? first.substring(0, 137) + "..." : first;
    }

    private static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while joining the concurrency harness", e);
        }
    }

    private static final class Attempt {
        private boolean committed;
        private boolean revoked;
        private RuntimeException error;
    }
}
