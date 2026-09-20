package io.forest.cdm.contactpoint.internal;

import io.forest.cdm.shared.Region;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Module-internal persistence for {@code contact_point} (UK-pinned, region-local). */
@Repository
public class JdbcContactPointRepository implements ContactPointRepository {

    private static final Region HOME_REGION = Region.UK;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcContactPointRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, UUID partyId, String contactType, String contactValue) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("partyId", partyId)
                .addValue("contactType", contactType)
                .addValue("contactValue", contactValue)
                .addValue("crdbRegion", HOME_REGION.sqlName());
        jdbc.update("""
                INSERT INTO contact_point (id, party_id, contact_type, contact_value, crdb_region)
                VALUES (:id, :partyId, :contactType, :contactValue, :crdbRegion)
                """, params);
    }

    public java.util.List<io.forest.cdm.contactpoint.ContactPointRow> findForParty(java.util.UUID partyId) {
        return jdbc.query(
                "SELECT contact_type, contact_value FROM contact_point WHERE party_id = :partyId",
                new MapSqlParameterSource("partyId", partyId),
                (rs, rowNum) -> new io.forest.cdm.contactpoint.ContactPointRow(
                        rs.getString("contact_type"), rs.getString("contact_value")));
    }

    public Map<Region, Long> countsByRegion() {
        Map<Region, Long> counts = new EnumMap<>(Region.class);
        jdbc.query("SELECT crdb_region::string AS region, count(*) AS total FROM contact_point GROUP BY crdb_region",
                rs -> {
                    Region region = Region.fromSqlName(rs.getString("region"));
                    if (region != null) {
                        counts.put(region, rs.getLong("total"));
                    }
                });
        return counts;
    }
}
