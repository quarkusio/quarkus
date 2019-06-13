package io.quarkus.it.main;

import javax.inject.Inject;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import io.quarkus.it.health.SimpleHealthCheck;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.SubstrateTest;

@QuarkusTest
public class HealthCheckTestCase {

    @Inject
    @Health
    SimpleHealthCheck checkks;

    @Test
    public void testInjection() {
        Assumptions.assumeFalse(getClass().isAnnotationPresent(SubstrateTest.class));
        Assert.assertTrue(checkks.call().getState() == HealthCheckResponse.State.UP);
    }
}
