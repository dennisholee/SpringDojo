package io.forest.cdm.migration.internal;

import io.forest.cdm.migration.LegacyCustomer;

import java.time.Duration;
import java.util.List;

/**
 * Outbound port for the legacy document store, owned by the {@code migration} module.
 *
 * <p>Implemented by {@link LegacyMongoRepository} against MongoDB. Keeping the port narrow matters:
 * the catch-up rules in {@code MigrationService} are written against this interface, so the change
 * stream can be replaced - or faked in a unit test - without touching the migration logic.
 */
public interface LegacyCustomerStore {

    /** Inserts or replaces a legacy customer, keyed by legal name. */
    void upsert(LegacyCustomer customer);

    List<LegacyCustomer> findAll();

    /**
     * Drains changes recorded since the last persisted resume token.
     *
     * @throws IllegalStateException if change streams cannot be opened, which the caller treats as
     *         a signal to fall back to a full sweep
     */
    List<LegacyCustomer> drainChangedCustomers(Duration maxAwait);
}
