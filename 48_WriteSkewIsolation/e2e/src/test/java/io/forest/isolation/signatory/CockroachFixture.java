package io.forest.isolation.signatory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;

/**
 * Boots a real single-node CockroachDB (strict serializable) and exposes a {@link SignatoryStore}
 * over plain JDBC. The init service applies {@code docker/init-cockroach.sql}, so readiness means
 * "the schema exists", not merely "the port answers".
 */
final class CockroachFixture {

    private static final String URL =
            "jdbc:postgresql://localhost:26257/cdm?sslmode=disable&user=root";
    private static final String COMPOSE_FILE = Compose.file("docker-compose.cockroach.yml");
    private static final String COMPOSE_PROJECT = "write-skew-cockroach";

    private static SignatoryStore store;

    private CockroachFixture() {
    }

    /** Boots the engine on first use, then reuses it for the rest of the suite. */
    static synchronized SignatoryStore store() {
        if (store == null) {
            Compose.up(COMPOSE_PROJECT, COMPOSE_FILE);
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> Compose.down(COMPOSE_PROJECT, COMPOSE_FILE)));
            awaitSchema();
            store = new JdbcSignatoryStore(URL);
            reset();
        }
        return store;
    }

    /** Resets party P1 to exactly two authorized signatories and a zeroed counter. */
    static void reset() {
        try (Connection connection = DriverManager.getConnection(URL);
             Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM signatory");
            statement.execute("INSERT INTO signatory (id, party_id, authorized) VALUES "
                    + "('alice', 'P1', true), ('bob', 'P1', true)");
            statement.execute("UPDATE party_counter SET seq = 0 WHERE party_id = 'P1'");
        } catch (SQLException e) {
            throw new RuntimeException("Could not reset the CockroachDB seed", e);
        }
    }

    /**
     * The committed rows that back the invariant, rendered for the evidence report. Read on its own
     * connection, so it reflects exactly what both concurrent transactions left behind.
     */
    static String snapshot() {
        try (Connection connection = DriverManager.getConnection(URL);
             Statement statement = connection.createStatement()) {
            StringBuilder rows = new StringBuilder();
            try (ResultSet result = statement.executeQuery(
                    "SELECT id, authorized FROM signatory ORDER BY id")) {
                while (result.next()) {
                    rows.append(result.getString("id")).append('=').append(result.getBoolean("authorized"))
                            .append(' ');
                }
            }
            String seq = "<none>";
            try (ResultSet result = statement.executeQuery(
                    "SELECT seq FROM party_counter WHERE party_id = 'P1'")) {
                if (result.next()) {
                    seq = String.valueOf(result.getInt(1));
                }
            }
            return "signatory[" + rows.toString().strip() + "] party_counter.seq=" + seq;
        } catch (SQLException e) {
            return "<snapshot unavailable: " + e.getMessage() + ">";
        }
    }

    private static void awaitSchema() {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(3));
        while (Instant.now().isBefore(deadline)) {
            try (Connection connection = DriverManager.getConnection(URL);
                 Statement statement = connection.createStatement()) {
                statement.executeQuery("SELECT count(*) FROM signatory").close();
                return;
            } catch (SQLException schemaPending) {
                // the init service has not applied the schema yet
            }
            sleep();
        }
        throw new IllegalStateException("The CockroachDB schema was not applied in time");
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
