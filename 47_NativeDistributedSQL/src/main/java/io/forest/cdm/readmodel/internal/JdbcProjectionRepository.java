package io.forest.cdm.readmodel.internal;

import io.forest.cdm.readmodel.Customer360View;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Module-internal persistence for the {@code customer_360} projection table. */
@Repository
public class JdbcProjectionRepository implements ProjectionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcProjectionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(Customer360View view) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("partyId", view.partyId())
                .addValue("legalName", view.legalName())
                .addValue("market", view.market())
                .addValue("lineOfBusiness", view.lineOfBusiness())
                .addValue("contactPoint", view.contactPoint())
                .addValue("accountNumbers", view.accountNumbers());
        jdbc.update("""
                INSERT INTO customer_360
                    (party_id, legal_name, market, line_of_business, contact_point, account_numbers)
                VALUES
                    (:partyId, :legalName, :market, :lineOfBusiness, :contactPoint, :accountNumbers)
                ON CONFLICT (party_id) DO UPDATE SET
                    legal_name = excluded.legal_name,
                    market = excluded.market,
                    line_of_business = excluded.line_of_business,
                    contact_point = excluded.contact_point,
                    account_numbers = excluded.account_numbers,
                    projected_at = now()
                """, params);
    }

    public Optional<Customer360View> find(UUID partyId) {
        List<Customer360View> rows = jdbc.query(
                """
                SELECT party_id, legal_name, market, line_of_business, contact_point, account_numbers
                FROM customer_360 WHERE party_id = :partyId
                """,
                new MapSqlParameterSource("partyId", partyId),
                (rs, rowNum) -> new Customer360View(
                        rs.getObject("party_id", UUID.class),
                        rs.getString("legal_name"),
                        rs.getString("market"),
                        rs.getString("line_of_business"),
                        rs.getString("contact_point"),
                        rs.getString("account_numbers")));
        return rows.stream().findFirst();
    }

    public long count() {
        Long total = jdbc.queryForObject("SELECT count(*) FROM customer_360",
                new MapSqlParameterSource(), Long.class);
        return total == null ? 0L : total;
    }
}
