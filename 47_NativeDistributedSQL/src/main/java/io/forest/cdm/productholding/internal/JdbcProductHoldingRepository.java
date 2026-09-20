package io.forest.cdm.productholding.internal;

import io.forest.cdm.shared.Region;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Module-internal persistence for {@code product_holding}.
 *
 * <p>Rows are pinned to the {@code hk} region via the implicit {@code crdb_region} column.
 * Contrast with {@code PartyRepository}, which pins to {@code uk}: that difference is what makes
 * an onboarding transaction span regions.
 */
@Repository
public class JdbcProductHoldingRepository implements ProductHoldingRepository {

    /** Market data is homed in the market region. */
    private static final Region HOME_REGION = Region.HK;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcProductHoldingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, UUID partyId, String productType, String accountNumber, String market) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("partyId", partyId)
                .addValue("productType", productType)
                .addValue("accountNumber", accountNumber)
                .addValue("market", market)
                .addValue("crdbRegion", HOME_REGION.sqlName());
        jdbc.update("""
                INSERT INTO product_holding (id, party_id, product_type, account_number, market, crdb_region)
                VALUES (:id, :partyId, :productType, :accountNumber, :market, :crdbRegion)
                """, params);
    }

    public java.util.List<io.forest.cdm.productholding.ProductHoldingRow> findForParty(java.util.UUID partyId) {
        return jdbc.query(
                "SELECT account_number, market FROM product_holding WHERE party_id = :partyId",
                new MapSqlParameterSource("partyId", partyId),
                (rs, rowNum) -> new io.forest.cdm.productholding.ProductHoldingRow(
                        rs.getString("account_number"), rs.getString("market")));
    }

    public Map<Region, Long> countsByRegion() {
        Map<Region, Long> counts = new EnumMap<>(Region.class);
        jdbc.query("SELECT crdb_region::string AS region, count(*) AS total FROM product_holding GROUP BY crdb_region",
                rs -> {
                    Region region = Region.fromSqlName(rs.getString("region"));
                    if (region != null) {
                        counts.put(region, rs.getLong("total"));
                    }
                });
        return counts;
    }
}
