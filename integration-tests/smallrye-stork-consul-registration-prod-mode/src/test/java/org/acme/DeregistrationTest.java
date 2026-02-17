package org.acme;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@DisabledOnOs(OS.WINDOWS)
@TestProfile(DeregistrationTest.DeregistrationConfigProfile.class)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class DeregistrationTest {

    @RegisterExtension
    static final QuarkusProdModeTest app = new QuarkusProdModeTest()
            .setApplicationName("consul-deregistration-test")
            .setApplicationVersion("1.0").setRun(true);

    public static class DeregistrationConfigProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "deregistration";
        }
    }

    @Test
    public void testDeregistrationAfterShutdown() throws Exception {

        // Ensure service is registered
        RestAssured.get("http://localhost:8500/v1/agent/service/consul-deregistration-test")
                .then()
                .statusCode(200);

        // Stop the app
        app.stop();

        // Wait until service is deregistered (404)
        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.get("http://localhost:8500/v1/agent/service/consul-deregistration-test")
                        .then()
                        .statusCode(404));

    }

}
