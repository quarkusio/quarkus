package io.quarkus.it.main;

import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import io.quarkus.it.health.SimpleHealthCheck;
import io.quarkus.test.junit.NativeImageTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.SubstrateTest;

@QuarkusTest
public class HealthCheckTestCase {

    @Inject
    @Liveness
    SimpleHealthCheck checkks;

    @Test
    public void testInjection() {
        Assumptions.assumeFalse(getClass().isAnnotationPresent(SubstrateTest.class));
        Assumptions.assumeFalse(getClass().isAnnotationPresent(NativeImageTest.class));
        Assert.assertTrue(checkks.call().getState() == HealthCheckResponse.State.UP);
    }
}
