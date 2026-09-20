package io.forest.cdm.party.internal;

import io.forest.cdm.shared.Region;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Module-internal persistence for {@code party}. Not part of the module's public API.
 *
 * <p>Rows are pinned to the {@code uk} region via the implicit {@code crdb_region} column of the
 * {@code REGIONAL BY ROW} table, so all party reads and writes are region-local.
 */
@Repository
public class JdbcPartyRepository implements PartyRepository {

    /** Party is a UK-mastered golden record: all rows are homed in the UK region. */
    private static final Region HOME_REGION = Region.UK;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPartyRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, String partyType, String legalName, Region masterRegion) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("partyType", partyType)
                .addValue("legalName", legalName)
                .addValue("masterRegion", masterRegion.sqlName())
                .addValue("crdbRegion", HOME_REGION.sqlName());
        jdbc.update("""
                INSERT INTO party (id, party_type, legal_name, master_region, crdb_region)
                VALUES (:id, :partyType, :legalName, :masterRegion, :crdbRegion)
                """, params);
    }

    public boolean exists(UUID id) {
        Integer matches = jdbc.queryForObject(
                "SELECT count(*) FROM party WHERE id = :id",
                new MapSqlParameterSource("id", id),
                Integer.class);
        return matches != null && matches > 0;
    }

    public boolean existsByLegalName(String legalName) {
        Integer matches = jdbc.queryForObject(
                "SELECT count(*) FROM party WHERE legal_name = :legalName",
                new MapSqlParameterSource("legalName", legalName),
                Integer.class);
        return matches != null && matches > 0;
    }

    public long countByLegalNamePrefix(String prefix) {
        Long matches = jdbc.queryForObject(
                "SELECT count(*) FROM party WHERE legal_name LIKE :prefix",
                new MapSqlParameterSource("prefix", prefix + "%"),
                Long.class);
        return matches == null ? 0L : matches;
    }

    public java.util.List<io.forest.cdm.party.PartyRow> listParties(int limit) {
        return jdbc.query(
                "SELECT id, legal_name FROM party ORDER BY legal_name LIMIT :limit",
                new MapSqlParameterSource("limit", limit),
                (rs, rowNum) -> new io.forest.cdm.party.PartyRow(
                        rs.getObject("id", java.util.UUID.class), rs.getString("legal_name")));
    }

    public Map<Region, Long> countsByRegion() {
        Map<Region, Long> counts = new EnumMap<>(Region.class);
        jdbc.query("SELECT crdb_region::string AS region, count(*) AS total FROM party GROUP BY crdb_region",
                rs -> {
                    Region region = Region.fromSqlName(rs.getString("region"));
                    if (region != null) {
                        counts.put(region, rs.getLong("total"));
                    }
                });
        return counts;
    }
}
