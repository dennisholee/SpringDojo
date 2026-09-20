package io.forest.cdm.migration;

import io.forest.cdm.migration.internal.LegacyCustomerStore;
import io.forest.cdm.migration.internal.MigrationWriter;
import io.forest.cdm.party.PartyService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the migration from the legacy document store to Distributed SQL.
 *
 * <p>Three phases, all idempotent:
 * <ol>
 *     <li><b>backfill</b> - a full sweep that creates a CDM counterpart for every legacy customer
 *     that lacks one.</li>
 *     <li><b>dual-write</b> - the strangler phase, where the new system writes to the CDM and
 *     mirrors the document into the legacy store.</li>
 *     <li><b>reconcile</b> - applies changes recorded by the MongoDB change stream since the last
 *     resume token, then sweeps to guarantee convergence even if the stream is unavailable.</li>
 * </ol>
 *
 * <p>Only customers whose legal name begins with {@value #PREFIX} are reconciled and counted, so
 * the migration proof of concept is self-scoped and cannot be perturbed by the other scenarios.
 */
@Service
public class MigrationService {

    public static final String PREFIX = "MIG-";

    private static final Duration CHANGE_STREAM_WINDOW = Duration.ofSeconds(3);

    private final LegacyCustomerStore legacy;
    private final MigrationWriter writer;
    private final PartyService partyService;

    private volatile String lastMechanism = "none";

    public MigrationService(LegacyCustomerStore legacy, MigrationWriter writer, PartyService partyService) {
        this.legacy = legacy;
        this.writer = writer;
        this.partyService = partyService;
    }

    /** Simulates the legacy system writing to its own store. */
    public void recordLegacyWrite(LegacyCustomer customer) {
        legacy.upsert(customer);
    }

    /** Strangler dual-write: the new system writes to the CDM and mirrors into the legacy store. */
    public UUID dualWrite(LegacyCustomer customer) {
        UUID partyId = writer.write(customer);
        legacy.upsert(customer);
        return partyId;
    }

    /** Full sweep. Returns how many CDM counterparts it created. */
    public int backfill() {
        int created = sweep();
        lastMechanism = "backfill";
        return created;
    }

    /**
     * Incremental catch-up from the change stream, followed by a sweep so convergence never
     * depends on the stream being available or complete.
     */
    public int reconcile() {
        boolean streamUsed = false;
        int applied = 0;
        try {
            applied = apply(legacy.drainChangedCustomers(CHANGE_STREAM_WINDOW));
            streamUsed = true;
        } catch (IllegalStateException e) {
            // Change streams need a replica set; the sweep below still converges.
        }
        applied += sweep();
        lastMechanism = streamUsed ? "change-stream" : "sweep-fallback";
        return applied;
    }

    public MigrationGap gap() {
        long legacyCount = scoped(legacy.findAll()).size();
        long cdmCount = partyService.countByLegalNamePrefix(PREFIX);
        return new MigrationGap(legacyCount, cdmCount, legacyCount - cdmCount, lastMechanism);
    }

    private int apply(List<LegacyCustomer> customers) {
        int created = 0;
        for (LegacyCustomer customer : scoped(customers)) {
            if (!partyService.existsByLegalName(customer.legalName())) {
                writer.write(customer);
                created++;
            }
        }
        return created;
    }

    private int sweep() {
        return apply(legacy.findAll());
    }

    private static List<LegacyCustomer> scoped(List<LegacyCustomer> customers) {
        return customers.stream()
                .filter(customer -> customer.legalName() != null
                        && customer.legalName().startsWith(PREFIX))
                .toList();
    }
}
