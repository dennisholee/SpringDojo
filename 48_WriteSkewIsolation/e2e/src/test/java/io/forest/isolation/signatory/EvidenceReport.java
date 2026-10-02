package io.forest.isolation.signatory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects one verdict row per scenario and, at the end of the suite, prints and persists the
 * consolidated verdict table — the decision-maker artifact of the run. The rows are derived from the
 * observed end state (not from the expected one), so a regression shows up as a changed verdict.
 */
final class EvidenceReport {

    private static final List<Row> ROWS = Collections.synchronizedList(new ArrayList<>());
    private static final Path OUTPUT = Path.of("target", "evidence", "verdict.md");

    private EvidenceReport() {
    }

    static void record(String engine, String isolation, String scenario, String result,
                       int finalAuthorized, String verdict) {
        ROWS.add(new Row(engine, isolation, scenario, result, finalAuthorized, verdict));
    }

    /** Prints the markdown verdict table and writes it to {@code target/evidence/verdict.md}. */
    static synchronized void write() {
        String table = render();
        System.out.println();
        System.out.println("=== WRITE-SKEW VERDICT TABLE " + "=".repeat(56));
        System.out.println(table);
        System.out.println("=== artifacts: target/cucumber-reports/cucumber.{json,html}, "
                + "target/evidence/verdict.md ===");
        try {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, table, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + OUTPUT.toAbsolutePath(), e);
        }
    }

    private static String render() {
        StringBuilder markdown = new StringBuilder();
        markdown.append("| Engine | Isolation | Scenario | Result | Final `authorized` | Verdict |\n");
        markdown.append("| --- | --- | --- | --- | --- | --- |\n");
        for (Row row : ROWS) {
            markdown.append("| ").append(row.engine())
                    .append(" | ").append(row.isolation())
                    .append(" | ").append(row.scenario())
                    .append(" | ").append(row.result())
                    .append(" | ").append(row.finalAuthorized())
                    .append(" | ").append(row.verdict())
                    .append(" |\n");
        }
        return markdown.toString();
    }

    private record Row(String engine, String isolation, String scenario, String result,
                       int finalAuthorized, String verdict) {
    }
}
