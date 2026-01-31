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
@TestProfile(MultipleRegistrarsConfigRegistrationTest.MultipleConfigProfile.class)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class MultipleRegistrarsConfigRegistrationTest {

    public static class MultipleConfigProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "multiple";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.stork.red-service.service-registrar.type", "consul",
                    "quarkus.stork.red-service.service-registrar.ip-address", "145.123.145.145",
                    "quarkus.stork.blue-service.service-registrar.type", "consul",
                    "quarkus.stork.blue-service.service-registrar.ip-address", "145.123.145.157");
        }

    }

    @Test
    public void test() {
        RestAssured.get("http://localhost:8500/v1/agent/service/red-service")
                .then()
                .statusCode(200)
                .body(containsString("\"Service\": \"red-service\""),
                        containsString("\"145.123.145.145\""));

        RestAssured.get("http://localhost:8500/v1/agent/service/blue-service")
                .then()
                .statusCode(200)
                .body(containsString("\"Service\": \"blue-service\""),
                        containsString("\"145.123.145.157\""));

    }

}
