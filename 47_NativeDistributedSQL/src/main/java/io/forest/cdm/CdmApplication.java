package io.forest.cdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Customer Data Management (CDM) modular monolith.
 *
 * <p>Every direct sub-package of {@code io.forest.cdm} is a Spring Modulith module:
 * <ul>
 *     <li>{@code party}, {@code relationship}, {@code contactpoint} - UK Core Domains</li>
 *     <li>{@code productholding} - HK Market Domain</li>
 *     <li>{@code onboarding} - composite use case wiring the modules together in-process</li>
 *     <li>{@code shared} - cross-cutting value types (e.g. {@link io.forest.cdm.shared.Region})</li>
 * </ul>
 * {@code ModularityTests} fails the build if a module reaches into another module's internals.
 */
@SpringBootApplication
public class CdmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CdmApplication.class, args);
    }
}
