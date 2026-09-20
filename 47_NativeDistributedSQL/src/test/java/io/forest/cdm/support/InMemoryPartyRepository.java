package io.forest.cdm.support;

import io.forest.cdm.party.PartyRow;
import io.forest.cdm.party.internal.PartyRepository;
import io.forest.cdm.shared.Region;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hand-written fake for the {@code party} outbound port, shared by unit tests.
 *
 * <p>Deliberately not a mocking framework: this is a real, tiny in-memory adapter. It is what the
 * port extraction bought - services can now be tested without Spring, without a database and
 * without mocks.
 */
public class InMemoryPartyRepository implements PartyRepository {

    private final Map<UUID, String> names = new LinkedHashMap<>();
    private final Map<UUID, Region> regions = new LinkedHashMap<>();

    @Override
    public void insert(UUID id, String partyType, String legalName, Region masterRegion) {
        names.put(id, legalName);
        regions.put(id, masterRegion);
    }

    @Override
    public boolean exists(UUID id) {
        return names.containsKey(id);
    }

    @Override
    public boolean existsByLegalName(String legalName) {
        return names.containsValue(legalName);
    }

    @Override
    public long countByLegalNamePrefix(String prefix) {
        return names.values().stream().filter(name -> name.startsWith(prefix)).count();
    }

    @Override
    public List<PartyRow> listParties(int limit) {
        return names.entrySet().stream()
                .limit(limit)
                .map(entry -> new PartyRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public Map<Region, Long> countsByRegion() {
        Map<Region, Long> counts = new EnumMap<>(Region.class);
        regions.values().forEach(region -> counts.merge(region, 1L, Long::sum));
        return counts;
    }

    /** Snapshot of the stored legal names, for assertions. */
    public Map<UUID, String> storedNames() {
        return Map.copyOf(names);
    }

    /** Snapshot of the stored master regions, for assertions. */
    public Map<UUID, Region> storedRegions() {
        return Map.copyOf(regions);
    }
}
