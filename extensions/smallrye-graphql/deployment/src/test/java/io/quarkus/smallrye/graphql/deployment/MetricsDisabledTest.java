package io.quarkus.smallrye.graphql.deployment;

import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify that GraphQL metrics are disabled by default even
 * if the SmallRye Metrics extension is present.
 */
public class MetricsDisabledTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            BusinessException.class, TestUnion.class, TestUnionMember.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry metricRegistry;

    @Test
    public void verifyMetricsAreNotRegistered() {
        SimpleTimer metric = metricRegistry.getSimpleTimers().get(new MetricID("mp_graphql_Query_ping"));
        assertNull(metric, "Metrics should not be registered");
    }

}
