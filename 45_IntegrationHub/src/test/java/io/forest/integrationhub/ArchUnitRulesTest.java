package io.forest.integrationhub;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import io.forest.integrationhub.core.GoodCoreClass;
import io.forest.integrationhub.core.BadCoreClass;
import io.forest.integrationhub.adapters.web.SamplePortImpl;
import io.forest.integrationhub.ports.SamplePort;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArchUnitRulesTest {

    private static final String ROOT = System.getProperty("archunit.root.package", "io.forest.integrationhub");

    private JavaClasses importProductionClasses() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages(ROOT);
    }

    private ArchRule coreAllowedDepsRule() {
        return classes()
            .that().resideInAPackage(ROOT + ".core..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                ROOT + ".core..",
                ROOT + ".ports..",
                "java..",
                "javax..",
                "jakarta..",
                "org.slf4j.."
            )
            .allowEmptyShould(true);
    }

    @Test
    void core_should_only_depend_on_allowed() {
        JavaClasses classes = importProductionClasses();
        coreAllowedDepsRule().check(classes);
    }

    @Test
    void adapters_should_only_depend_on_allowed() {
        JavaClasses classes = importProductionClasses();

        ArchRule rule = classes()
            .that().resideInAPackage(ROOT + ".adapters..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                ROOT + ".adapters..", ROOT + ".ports..", "java..", "javax..", "jakarta..", "org.slf4j..", "com.fasterxml.jackson..", "org.apache..", "org.springframework.."
            )
            .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void ports_should_only_depend_on_allowed() {
        JavaClasses classes = importProductionClasses();

        ArchRule rule = classes()
            .that().resideInAPackage(ROOT + ".ports..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(ROOT + ".ports..", "java..", "javax..", "jakarta..")
            .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void seeded_violation_should_be_detected() {
        JavaClasses classes = new ClassFileImporter().importClasses(
            BadCoreClass.class,
            SamplePortImpl.class,
            GoodCoreClass.class,
            SamplePort.class
        );

        assertThrows(AssertionError.class, () -> coreAllowedDepsRule().check(classes));
    }

}
