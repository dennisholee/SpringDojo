package io.forest.cdm.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Boots the real two-region CockroachDB cluster that the end-to-end suite runs against.
 *
 * <p>There is no in-memory database and no fake: this starts the same Docker Compose topology
 * (and therefore the same {@code init-db.sql}) that a developer or CI pipeline would use, and the
 * application under test connects to it over the PostgreSQL wire protocol.
 *
 * <p>The topology is selectable so the same suite works on a laptop and in CI:
 * <pre>
 *   -Dcdm.e2e.compose=docker/docker-compose.lite.yml   (default, 2 regions x 2 nodes)
 *   -Dcdm.e2e.compose=docker/docker-compose.yml        (canonical, 2 regions x 3 nodes)
 * </pre>
 */
public final class CockroachClusterFixture {

    /**
     * Compose files that make up the test topology, comma separated. The default is a canonical
     * two-region CockroachDB cluster plus the legacy MongoDB replica set used by the migration PoC.
     */
    private static final String[] COMPOSE_FILES = System
            .getProperty("cdm.e2e.compose",
                    "docker/docker-compose.yml,docker/docker-compose.mongo.yml")
            .split(",");
    private static final int SQL_PORT = 26257;

    private static final String PROJECT = System.getProperty("cdm.e2e.project", "cdm-e2e");
    private static final int DEFAULT_HOST_PORT = 26257;
    private static final boolean REUSE_CLUSTER = Boolean.getBoolean("cdm.e2e.reuse");

    private static String jdbcUrl;
    private static boolean started;

    private CockroachClusterFixture() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        if (!dockerAvailable()) {
            throw new IllegalStateException(
                    "The end-to-end suite needs a running Docker daemon (it boots a real "
                            + "two-region CockroachDB cluster). Start Docker Desktop and re-run "
                            + "`mvn verify -Pe2e`. For unit tests only, run `mvn test`.");
        }

        // The Docker Compose v2 plugin is invoked directly. The legacy `docker-compose` binary is
        // not guaranteed to be on PATH, and Testcontainers' bundled Compose V1 image cannot parse
        // a modern compose file.
        // With -Dcdm.e2e.reuse=true an already-running cluster is reused, which makes iterating
        // locally much faster (start it once with `docker compose -f ... up -d`).
        if (!REUSE_CLUSTER) {
            compose("up", "-d", "--remove-orphans");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    compose("down", "-v", "--remove-orphans");
                } catch (RuntimeException ignored) {
                    // best-effort teardown; never let the hook mask a real test failure
                }
            }));
        }

        int hostPort = REUSE_CLUSTER ? DEFAULT_HOST_PORT : resolveHostPort();
        jdbcUrl = "jdbc:postgresql://localhost:" + hostPort + "/cdm?sslmode=disable";
        awaitSchema(Duration.ofMinutes(5));
        started = true;
    }

    /** Stops a compose service. Used by the fault-injection scenario. */
    public static void stopService(String service) {
        compose("stop", service);
    }

    /** Starts a previously stopped compose service. */
    public static void startService(String service) {
        compose("start", service);
    }

    /** A quick, non-invasive Docker check: no client library, no side-car container. */
    private static boolean dockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            return process.waitFor() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /** Runs {@code docker compose -p <project> -f <file> <args...>} and fails loudly on error. */
    private static void compose(String... args) {
        capture(args);
    }

    /** Runs docker compose and returns its combined output, failing loudly on a non-zero exit. */
    private static String capture(String... args) {
        List<String> command = baseCommand();
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("docker compose " + String.join(" ", args)
                        + " failed with exit code " + exitCode + ":\n" + output);
            }
            return output;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running docker compose", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not run docker compose", e);
        }
    }

    /** Compose command prefix with every configured file, for a composed topology. */
    private static List<String> baseCommand() {
        List<String> command = new ArrayList<>(List.of("docker", "compose", "-p", PROJECT));
        for (String file : COMPOSE_FILES) {
            command.add("-f");
            command.add(file.trim());
        }
        return command;
    }

    /** @return the compose service names starting with the supplied prefix (e.g. {@code hk}). */
    public static List<String> servicesStartingWith(String prefix) {
        List<String> matches = new ArrayList<>();
        for (String line : capture("config", "--services").split("\\R")) {
            String name = line.trim();
            if (name.startsWith(prefix)) {
                matches.add(name);
            }
        }
        return matches;
    }

    /** Resolves the host port that the uk1 SQL endpoint is published on. */
    private static int resolveHostPort() {
        List<String> command = baseCommand();
        command.add("port");
        command.add("uk1");
        command.add(String.valueOf(SQL_PORT));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            process.waitFor();
            int separator = output.lastIndexOf(':');
            return separator < 0 ? DEFAULT_HOST_PORT : Integer.parseInt(output.substring(separator + 1).trim());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while resolving the SQL port", e);
        } catch (IOException | NumberFormatException e) {
            return DEFAULT_HOST_PORT;
        }
    }

    public static String jdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Waits until the bootstrap service has finished applying the geo-partitioned schema.
     * The cluster forms and the DDL runs asynchronously, so polling is the reliable signal.
     */
    private static void awaitSchema(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        SQLException lastFailure = null;

        while (Instant.now().isBefore(deadline)) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "root", "");
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT count(*) FROM information_schema.tables "
                                 + "WHERE table_name IN ('party', 'product_holding')")) {
                if (rs.next() && rs.getInt(1) == 2) {
                    return;
                }
            } catch (SQLException e) {
                lastFailure = e;
            }
            sleepOneSecond();
        }
        throw new IllegalStateException("The geo-partitioned schema was not applied in time", lastFailure);
    }

    private static void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the cluster", e);
        }
    }
}
