package org.acme;

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
            return "minimal";
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

        // Wait a moment for deregistration to occur
        Thread.sleep(500);

        // Check service is gone
        RestAssured.get("http://localhost:8500/v1/agent/service/consul-deregistration-test")
                .then()
                .statusCode(404);
    }

}
