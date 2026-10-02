package io.forest.isolation.signatory;

import java.util.function.Function;

/**
 * The outbound port. Each engine supplies a transaction handle whose isolation level is whatever
 * that engine defaults to; this PoC never overrides it.
 */
public interface SignatoryStore {

    /**
     * Runs {@code action} inside a single transaction against the backing engine.
     *
     * @return whatever {@code action} returned, once the transaction has committed
     */
    <T> T inTransaction(Function<Tx, T> action);
}
