package io.quarkus.smallrye.metrics.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.not;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * The same test as {@link MetricsFromExtensionTestCase}, except this time
 * the quarkus.smallrye-metrics.extensions.enabled flag is used to turn off
 * metrics from extensions, so we verify that no metrics will be registered
 * even though the extension produced some {@link io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem} items.
 */
public class MetricsFromExtensionDisabledTestCase extends MetricsFromExtensionTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MetricResource.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-metrics.extensions.enabled=false"),
                            "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    @Test
    public void testBaseRegistryType() {
        String[] metricNames = RestAssured.when().get("/get-counters-base").then().extract().as(String[].class);
        assertThat(metricNames,
                not(hasItemInArray("io.quarkus.smallrye.metrics.test.MetricResource.countMeInBaseScope")));
        assertThat(metricNames, not(emptyArray())); // regular base metrics should still be present
    }

    @Test
    public void testVendorRegistryType() {
        String[] metricNames = RestAssured.when().get("/get-counters").then().extract().as(String[].class);
        assertThat(metricNames, emptyArray());
    }

}
