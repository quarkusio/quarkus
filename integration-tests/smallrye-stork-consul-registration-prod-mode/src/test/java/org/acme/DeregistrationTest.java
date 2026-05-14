package org.acme;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
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
        String serviceId = findServiceId("consul-deregistration-test");
        assertNotNull(serviceId, "Service 'consul-deregistration-test' should be registered in Consul");

        RestAssured.get("http://localhost:8500/v1/agent/service/" + serviceId)
                .then()
                .statusCode(200)
                .body(containsString("\"ServiceName\": \"consul-deregistration-test\""));

        app.stop();

        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured
                        .get("http://localhost:8500/v1/agent/service/" + serviceId)
                        .then()
                        .statusCode(404));
    }

    private String findServiceId(String serviceName) {
        Map<String, ?> services = RestAssured.get("http://localhost:8500/v1/agent/services")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap(".");
        return services.keySet().stream()
                .filter(id -> id.startsWith(serviceName))
                .findFirst()
                .orElse(null);
    }

}