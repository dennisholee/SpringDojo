package io.forest.cdm.relationship.internal;

import io.forest.cdm.shared.Region;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Module-internal persistence for {@code relationship} (UK-pinned, region-local). */
@Repository
public class JdbcRelationshipRepository implements RelationshipRepository {

    private static final Region HOME_REGION = Region.UK;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRelationshipRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, UUID partyId, String market, String lineOfBusiness) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("partyId", partyId)
                .addValue("market", market)
                .addValue("lineOfBusiness", lineOfBusiness)
                .addValue("crdbRegion", HOME_REGION.sqlName());
        jdbc.update("""
                INSERT INTO relationship (id, party_id, market, line_of_business, crdb_region)
                VALUES (:id, :partyId, :market, :lineOfBusiness, :crdbRegion)
                """, params);
    }

    public java.util.List<io.forest.cdm.relationship.RelationshipRow> findForParty(java.util.UUID partyId) {
        return jdbc.query(
                "SELECT market, line_of_business FROM relationship WHERE party_id = :partyId",
                new MapSqlParameterSource("partyId", partyId),
                (rs, rowNum) -> new io.forest.cdm.relationship.RelationshipRow(
                        rs.getString("market"), rs.getString("line_of_business")));
    }

    public Map<Region, Long> countsByRegion() {
        Map<Region, Long> counts = new EnumMap<>(Region.class);
        jdbc.query("SELECT crdb_region::string AS region, count(*) AS total FROM relationship GROUP BY crdb_region",
                rs -> {
                    Region region = Region.fromSqlName(rs.getString("region"));
                    if (region != null) {
                        counts.put(region, rs.getLong("total"));
                    }
                });
        return counts;
    }
}
