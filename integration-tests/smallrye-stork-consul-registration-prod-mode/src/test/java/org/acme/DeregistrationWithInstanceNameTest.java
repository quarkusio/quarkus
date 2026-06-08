package org.acme;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

/**
 * Verifies that when a custom {@code instance-name} is configured, the service is deregistered
 * from Consul using that exact instance ID after application shutdown.
 */
@DisabledOnOs(OS.WINDOWS)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class DeregistrationWithInstanceNameTest {

    static final String APP_NAME = "consul-instance-name-deregistration-test";
    static final String INSTANCE_NAME = "my-fixed-instance-id";

    @RegisterExtension
    static final QuarkusProdModeTest app = new QuarkusProdModeTest()
            .setApplicationName(APP_NAME)
            .setApplicationVersion("1.0")
            .overrideConfigKey("quarkus.stork." + APP_NAME + ".service-registrar.instance-name", INSTANCE_NAME)
            .setRun(true);

    @Test
    public void testDeregistrationUsesConfiguredInstanceName() throws Exception {
        RestAssured.get("http://localhost:8500/v1/agent/service/" + INSTANCE_NAME)
                .then()
                .statusCode(200);

        app.stop();

        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured
                        .get("http://localhost:8500/v1/agent/service/" + INSTANCE_NAME)
                        .then()
                        .statusCode(404));
    }
}