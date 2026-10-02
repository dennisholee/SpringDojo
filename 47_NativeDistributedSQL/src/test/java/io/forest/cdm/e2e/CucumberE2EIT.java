package io.forest.cdm.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Entry point of the end-to-end suite.
 *
 * <p>Run with {@code mvn verify -Pe2e}. The class name ends in {@code IT} so that the default
 * {@code mvn test} run (unit and architecture tests only) does not start Docker.
 *
 * <p>Every feature is tagged {@code @PoC-<id>} so the JSON report can be sliced into one report
 * per proof of concept.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.forest.cdm.e2e")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, "
                + "json:target/cucumber-reports/cucumber.json, "
                + "html:target/cucumber-reports/cucumber.html")
public class CucumberE2EIT {
}
