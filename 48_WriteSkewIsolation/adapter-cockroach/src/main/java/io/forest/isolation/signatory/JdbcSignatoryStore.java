package io.forest.isolation.signatory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * {@link SignatoryStore} over plain JDBC (PostgreSQL wire protocol). The isolation level is left at
 * the engine default — CockroachDB is SERIALIZABLE — deliberately: with {@code DriverManager} there
 * is no framework quietly lowering it, so the choice stays visible.
 */
public final class JdbcSignatoryStore implements SignatoryStore {

    private final String url;

    public JdbcSignatoryStore(String url) {
        this.url = url;
    }

    @Override
    public <T> T inTransaction(Function<Tx, T> action) {
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);          // stays at SERIALIZABLE (engine default)
            try {
                T result = action.apply(new JdbcTx(connection));
                connection.commit();
                return result;
            } catch (Exception e) {
                rollback(connection);
                throw e instanceof RuntimeException runtime ? runtime : new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** A serialization failure leaves the transaction already aborted; rollback is best-effort. */
    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // nothing useful to add: the original failure is the one that matters
        }
    }

    private static final class JdbcTx implements Tx {

        private final Connection connection;

        private JdbcTx(Connection connection) {
            this.connection = connection;
        }

        @Override
        public int countAuthorized(String partyId) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT count(*) FROM signatory WHERE party_id = ? AND authorized")) {
                statement.setString(1, partyId);
                try (ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    return rows.getInt(1);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setAuthorized(String partyId, String signatoryId, boolean authorized) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE signatory SET authorized = ? WHERE id = ?")) {
                statement.setBoolean(1, authorized);
                statement.setString(2, signatoryId);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void touch(String partyId) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE party_counter SET seq = seq + 1 WHERE party_id = ?")) {
                statement.setString(1, partyId);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
