package io.forest.cdm;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.forest.cdm.shared.CdmDomainException;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture fitness functions for the hexagonal, per-bounded-context layout.
 *
 * <p>Complements {@link ModularityTests} (which enforces Spring Modulith's module boundaries):
 * these rules pin down the <em>layering</em> inside each module, which Modulith does not check.
 *
 * <p>Only production classes are analysed - the end-to-end suite legitimately talks to the database
 * directly, so test classes are excluded.
 */
class ArchitectureTests {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("io.forest.cdm");

    /** Web concerns belong to driving adapters, never to services or domain types. */
    @Test
    void webAdaptersLiveOnlyInWebPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("io.forest.cdm..")
                .and().resideOutsideOfPackage("..internal.web..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web..", "org.springframework.http..");
        rule.check(PRODUCTION_CLASSES);
    }

    /** The domain side depends on the port (interface), never on the concrete adapter. */
    @Test
    void domainTypesDoNotDependOnConcreteAdapters() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..internal..")
                .should().dependOnClassesThat().haveSimpleNameStartingWith("Jdbc");
        rule.check(PRODUCTION_CLASSES);
    }

    /** Domain types must not see the persistence technology at all. */
    @Test
    void domainTypesDoNotDependOnPersistenceTechnology() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..internal..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("java.sql..", "org.springframework.jdbc..", "com.mongodb..");
        rule.check(PRODUCTION_CLASSES);
    }

    /** CQRS: the command side must never reach into the query side. */
    @Test
    void writePathDoesNotDependOnTheReadModel() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "io.forest.cdm.party..",
                        "io.forest.cdm.relationship..",
                        "io.forest.cdm.contactpoint..",
                        "io.forest.cdm.productholding..",
                        "io.forest.cdm.onboarding..",
                        "io.forest.cdm.migration..")
                .should().dependOnClassesThat().resideInAPackage("io.forest.cdm.readmodel..");
        rule.check(PRODUCTION_CLASSES);
    }

    /** The shared kernel must stay a leaf: it cannot depend on any bounded context. */
    @Test
    void sharedKernelDoesNotDependOnAnyModule() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("io.forest.cdm.shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.forest.cdm.party..",
                        "io.forest.cdm.relationship..",
                        "io.forest.cdm.contactpoint..",
                        "io.forest.cdm.productholding..",
                        "io.forest.cdm.onboarding..",
                        "io.forest.cdm.migration..",
                        "io.forest.cdm.readmodel..");
        rule.check(PRODUCTION_CLASSES);
    }

    /** Commands and results may be application types, but the reverse direction is forbidden. */
    @Test
    void onlyWebAdaptersDependOnWebAdapters() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..internal.web..")
                .should().dependOnClassesThat().resideInAPackage("..internal.web..");
        rule.check(PRODUCTION_CLASSES);
    }

    /** HTTP DTO naming is a layer marker: only the web adapter may own Request/Result types. */
    @Test
    void httpDtosLiveInWebPackages() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Result")
                .should().resideInAPackage("..internal.web..");
        rule.check(PRODUCTION_CLASSES);
    }

    /** One base type for domain failures, so the web layer needs exactly one handler. */
    @Test
    void domainExceptionsShareOneBaseType() {
        ArchRule rule = classes()
                .that().resideInAPackage("io.forest.cdm..")
                .and().haveSimpleNameEndingWith("NotFoundException")
                .should().beAssignableTo(CdmDomainException.class);
        rule.check(PRODUCTION_CLASSES);
    }

    /** Guards against vacuous rules: an empty class set would make every rule pass silently. */
    @Test
    void theAnalysedClassSetIsNotEmpty() {
        if (PRODUCTION_CLASSES.size() < 10) {
            throw new AssertionError(
                    "Expected the production class set to be populated, found "
                            + PRODUCTION_CLASSES.size() + " classes");
        }
        boolean hasWebAdapter = PRODUCTION_CLASSES.stream()
                .anyMatch(javaClass -> javaClass.getName().endsWith("internal.web.OnboardingController"));
        if (!hasWebAdapter) {
            throw new AssertionError("Expected the OnboardingController web adapter to be analysed");
        }
    }
}
