package io.forest.isolation.signatory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The smallest wrapper that lets a fixture drive the Docker Compose v2 plugin. There is no mocking
 * layer and no side-car container: the engines under test are the real ones.
 *
 * <p>Each engine gets its own project name so the two Compose files (both named
 * {@code write-skew-poc}) never collide.
 */
final class Compose {

    private Compose() {
    }

    static void up(String project, String file) {
        requireDocker();
        run(project, file, "up", "-d", "--remove-orphans");
    }

    /** Best-effort teardown: a failure here must never mask a test result. */
    static void down(String project, String file) {
        try {
            run(project, file, "down", "-v", "--remove-orphans");
        } catch (RuntimeException ignored) {
            // the container may already be gone
        }
    }

    /**
     * Resolves a Compose file that lives in the reactor root's {@code docker/} directory. Surefire
     * runs with the {@code e2e} module directory as its working directory, so the file sits one level
     * up; {@code -Dwrite-skew.compose.dir} overrides the lookup for a stack started elsewhere.
     *
     * <p>Paths <em>inside</em> the resolved file stay relative to the file itself, as the Compose
     * specification requires, which is why an absolute {@code -f} argument is safe here.
     */
    static String file(String name) {
        String configured = System.getProperty("write-skew.compose.dir");
        Path directory = configured != null ? Path.of(configured) : Path.of("..", "docker");
        Path file = directory.resolve(name).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Compose file not found: " + file
                    + " - run the suite from the e2e module, or set -Dwrite-skew.compose.dir");
        }
        return file.toString();
    }

    private static void requireDocker() {
        try {
            Process process = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            if (process.waitFor() != 0) {
                throw new IllegalStateException("The Docker daemon is not reachable");
            }
        } catch (IOException e) {
            throw new IllegalStateException("The end-to-end suite needs a running Docker daemon", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while checking Docker", e);
        }
    }

    private static void run(String project, String file, String... args) {
        List<String> command = new ArrayList<>(List.of("docker", "compose", "-p", project, "-f", file));
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException(
                        "docker compose " + String.join(" ", args) + " exited with " + exit + ":\n" + output);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not run docker compose", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running docker compose", e);
        }
    }
}
