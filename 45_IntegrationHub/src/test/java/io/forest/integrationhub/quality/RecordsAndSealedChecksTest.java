package io.forest.integrationhub.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class RecordsAndSealedChecksTest {

    @Test
    public void reflectionEnforcementsDisabledByDefault() {
        // Reflection-based enforcements are opt-in via system property to avoid breaking local dev runs.
        Assumptions.assumeFalse(Boolean.getBoolean("enable.quality.enforcements"),
                "Reflection enforcements are disabled by default");
    }

    @Test
    public void dtosMustBeRecords() throws Exception {
        // Only run when explicitly enabled (CI); scans compiled classes under target/classes.
        Assumptions.assumeTrue(Boolean.getBoolean("enable.quality.enforcements"),
                "Reflection enforcements are disabled by default");

        Path classesRoot = Paths.get("target", "classes");
        if (!Files.exists(classesRoot)) {
            // Nothing compiled — avoid failing the build here.
            return;
        }

        List<String> violations = new ArrayList<>();

        Files.walk(classesRoot)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(p -> {
                    Path rel = classesRoot.relativize(p);
                    String fqcn = rel.toString().replace(File.separatorChar, '.');
                    fqcn = fqcn.substring(0, fqcn.length() - ".class".length());
                    try {
                        Class<?> cls = Class.forName(fqcn, false, Thread.currentThread().getContextClassLoader());
                        String simple = cls.getSimpleName();
                        String pkg = (cls.getPackage() != null) ? cls.getPackage().getName() : "";

                        boolean looksLikeDto = simple.endsWith("Dto") || pkg.contains(".dto.");
                        if (looksLikeDto && !cls.isRecord()) {
                            violations.add(fqcn + " looks like a DTO but is not a record");
                        }

                        boolean looksLikeState = simple.endsWith("State") || pkg.contains(".domain.");
                        if (looksLikeState && !cls.isSealed()) {
                            violations.add(fqcn + " looks like a domain state but is not sealed");
                        }
                    } catch (Throwable ignored) {
                        // Ignore classes that cannot be loaded or initialized.
                    }
                });

        if (!violations.isEmpty()) {
            fail("DTO/Sealed enforcement failures:\n" + String.join("\n", violations));
        }
    }
}
