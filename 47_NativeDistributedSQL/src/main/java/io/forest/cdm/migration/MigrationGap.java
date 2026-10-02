package io.forest.cdm.migration;

/**
 * The reconciliation gap between the legacy store and the Customer Data Management store.
 *
 * <p>{@code gap} is the number of legacy customers that have no counterpart yet. Cutover is only
 * safe while it reads zero.
 *
 * @param mechanism which catch-up mechanism last ran: {@code backfill}, {@code change-stream} or
 *                  {@code sweep-fallback} (used when change streams are unavailable)
 */
public record MigrationGap(
        long legacyCount,
        long cdmCount,
        long gap,
        String mechanism
) {
}
