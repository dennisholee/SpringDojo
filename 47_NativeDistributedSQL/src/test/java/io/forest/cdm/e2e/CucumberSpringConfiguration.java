package io.forest.cdm.e2e;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shares one real application instance across every scenario.
 *
 * <p>The Spring context is started against the real cluster booted by
 * {@link CockroachClusterFixture}; the static initialiser runs before the context is created so
 * the JDBC URL is available for property binding. Nothing is mocked or stubbed.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    static {
        CockroachClusterFixture.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CockroachClusterFixture::jdbcUrl);
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "");
    }
}
