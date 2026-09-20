package io.forest.cdm;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture fitness function.
 *
 * <p>Fails the build as soon as a module reaches into another module's {@code internal} package
 * (or a dependency cycle appears). This is what stops a modular monolith from decaying into a
 * big ball of mud, and is the trade-off we accept in exchange for dropping the network
 * boundaries of microservices.
 *
 * <p>This test needs no database: Spring Modulith inspects the compiled module structure.
 */
class ModularityTests {

    private static final ApplicationModules MODULES = ApplicationModules.of(CdmApplication.class);

    @Test
    void moduleBoundariesAreRespected() {
        MODULES.verify();
    }

    @Test
    void exposesTheExpectedModules() {
        List<String> moduleNames = new ArrayList<>();
        MODULES.forEach(module -> moduleNames.add(module.getName()));

        moduleNames.forEach(name -> System.out.println("module: " + name));

        assertThat(moduleNames).contains(
                "party", "relationship", "contactpoint", "productholding", "onboarding", "shared");
    }
}
