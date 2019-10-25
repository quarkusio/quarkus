package io.quarkus.smallrye.metrics.test;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MetricResource.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-metrics.extensions.enabled=false"),
                            "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    @Test
    public void test() {
        String[] metricNames = RestAssured.when().get("/get-counters").then().extract().as(String[].class);
        assertThat(metricNames, Matchers.emptyArray());
    }

}
