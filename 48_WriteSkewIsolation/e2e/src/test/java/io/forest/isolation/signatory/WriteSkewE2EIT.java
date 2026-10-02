package io.forest.isolation.signatory;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Entry point of the end-to-end suite. Runs both feature files against real MongoDB and real
 * CockroachDB containers; nothing is mocked. Enabled by the {@code e2e} Maven profile.
 *
 * <p>The package selector {@code features} is used rather than a classpath-resource selector: the
 * Cucumber engine warns against the latter for packaged features, and the package selector discovers
 * the same two {@code .feature} files without the warning.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.forest.isolation.signatory")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, json:target/cucumber-reports/cucumber.json, html:target/cucumber-reports/cucumber.html")
public class WriteSkewE2EIT {
}
