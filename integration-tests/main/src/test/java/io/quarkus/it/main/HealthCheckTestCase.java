package io.quarkus.it.main;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import io.quarkus.it.health.SimpleHealthCheck;
import io.quarkus.test.junit.DisabledOnNativeImage;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisabledOnNativeImage("This test is not meant to be ran in native mode as Quarkus does not yet support injection " +
        "in native " + "tests - see https://quarkus.io/guides/getting-started-testing#native-executable-testing")
public class HealthCheckTestCase {

    final SimpleHealthCheck simpleHealthCheck;

    public HealthCheckTestCase(@Liveness SimpleHealthCheck simpleHealthCheck) {
        this.simpleHealthCheck = simpleHealthCheck;
    }

    @Test
    public void testInjection() {
        Assert.assertTrue(simpleHealthCheck.call().getStatus() == HealthCheckResponse.Status.UP);
        Assert.assertTrue(simpleHealthCheck.call().getStatus() == HealthCheckResponse.Status.UP);
    }
}
