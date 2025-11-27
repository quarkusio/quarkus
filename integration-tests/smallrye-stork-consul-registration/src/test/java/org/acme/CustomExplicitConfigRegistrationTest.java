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
@TestProfile(CustomExplicitConfigRegistrationTest.CustomExplicitConfigProfile.class)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class CustomExplicitConfigRegistrationTest {

    public static class CustomExplicitConfigProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "minimal";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.stork.my-service.service-registrar.ip-address", "145.123.145.145",
                    "quarkus.stork.my-service.service-registrar.port", "9090");
        }

    }

    @Test
    public void test() {
        RestAssured.get("http://localhost:8500/v1/agent/service/my-service")
                .then()
                .statusCode(200)
                .body(containsString("\"Service\": \"my-service\""),
                        containsString("\"Port\": 9090"),
                        containsString("\"Address\": \"145.123.145.145\""));

    }

}
