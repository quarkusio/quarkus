package io.quarkus.smallrye.health.test;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.health.api.HealthGroup;

class ExpectedBeansUnitTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FailingHealthCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));
    @Inject
    @Any
    Instance<HealthCheck> checks;

    @Inject
    Instance<SmallRyeHealthReporter> reporters;

    private boolean isUnique(Instance<?> instances) {
        return !(instances.isAmbiguous() || instances.isUnsatisfied());
    }

    /**
     * Test that SmallRye Health Reporter is registered and unique
     */
    @Test
    void testReporterIsUnique() {
        Assertions.assertTrue(isUnique(reporters));
    }

    /**
     * Test that HealthCheck procedure beans are registered once
     */
    @Test
    void testHealthCheckIsUnique() {
        Assertions.assertTrue(isUnique(checks));
    }

    /**
     * Test metadata on HealthCheck procedure beans
     */
    @Test
    void testHealthCheckMetadata() {
        Instance<HealthCheck> selects;

        selects = checks.select(Liveness.Literal.INSTANCE);
        Assertions.assertTrue(isUnique(selects));

        selects = checks.select(Readiness.Literal.INSTANCE);
        Assertions.assertTrue(isUnique(selects));

        selects = checks.select(Startup.Literal.INSTANCE);
        Assertions.assertTrue(isUnique(selects));

        selects = checks.select(HealthGroup.Literal.of("group1"));
        Assertions.assertTrue(isUnique(selects));

        selects = checks.select(HealthGroup.Literal.of("group2"));
        Assertions.assertTrue(isUnique(selects));

        selects = checks.select(Liveness.Literal.INSTANCE,
                Readiness.Literal.INSTANCE,
                Startup.Literal.INSTANCE,
                HealthGroup.Literal.of("group1"),
                HealthGroup.Literal.of("group2"));
        Assertions.assertTrue(isUnique(selects));

        Assertions.assertTrue(checks.select(HealthGroup.Literal.of("group3")).isUnsatisfied());

    }

}
