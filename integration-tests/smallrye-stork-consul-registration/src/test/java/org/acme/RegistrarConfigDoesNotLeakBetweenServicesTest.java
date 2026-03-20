package org.acme;

import static org.hamcrest.CoreMatchers.containsString;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/**
 * A configuration block is defined for a single service (see Map returned by ConsulTestResource), but without the need to
 * indicate the registrar type.
 */

@DisabledOnOs(OS.WINDOWS)
@QuarkusTest
@TestProfile(RegistrarConfigDoesNotLeakBetweenServicesTest.RegistrarConfigDoesNotLeakProfile.class)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class RegistrarConfigDoesNotLeakBetweenServicesTest {

    public static class RegistrarConfigDoesNotLeakProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "multiple-stork";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.stork.red-service.service-registrar.type", "consul",
                    "quarkus.stork.blue-service.service-discovery.type", "consul");
        }

    }

    @Test
    public void test() {
        RestAssured.get("http://localhost:8500/v1/agent/service/red-service")
                .then()
                .statusCode(200)
                .body(containsString("\"Service\": \"red-service\""));

        RestAssured.get("http://localhost:8500/v1/agent/service/blue-service")
                .then()
                .statusCode(404);

    }

}
