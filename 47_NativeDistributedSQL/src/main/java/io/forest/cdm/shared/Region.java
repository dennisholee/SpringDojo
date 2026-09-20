package io.forest.cdm.shared;

/**
 * SQL region identifiers used for CockroachDB {@code REGIONAL BY ROW} placement.
 *
 * <p>These names MUST match the regions registered on the database, i.e. the {@code region=...}
 * values in the node localities of {@code docker/docker-compose.yml} that are then added via
 * {@code ALTER DATABASE cdm ADD REGION ...} in {@code docker/init-db.sql}.
 *
 * <p>They are also the literal values stored in the implicit {@code crdb_region} column of every
 * {@code REGIONAL BY ROW} table, which is what makes the residency proof possible:
 * a UK row physically lives on UK nodes and an HK row on HK nodes.
 */
public enum Region {

    UK("uk"),
    HK("hk");

    private final String sqlName;

    Region(String sqlName) {
        this.sqlName = sqlName;
    }

    public String sqlName() {
        return sqlName;
    }

    /**
     * @return the matching region, or {@code null} if the value does not map to a known region.
     */
    public static Region fromSqlName(String name) {
        for (Region region : values()) {
            if (region.sqlName.equalsIgnoreCase(name)) {
                return region;
            }
        }
        return null;
    }
}
