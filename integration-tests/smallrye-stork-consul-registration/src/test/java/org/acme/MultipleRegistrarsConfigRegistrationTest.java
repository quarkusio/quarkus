package org.acme;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
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
@Disabled("Require a Stork v3 update")
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
        RestAssured.get("http://localhost:8500/v1/catalog/service/red-service")
                .then()
                .statusCode(200)
                .body(containsString("\"ServiceName\": \"red-service\""),
                        containsString("\"ServiceAddress\": \"145.123.145.145\""))
                .body("ServiceID", hasItem(matchesPattern("^red-service::145.123.145.145::8080")));

        RestAssured.get("http://localhost:8500/v1/catalog/service/blue-service")
                .then()
                .statusCode(200)
                .body(containsString("\"ServiceName\": \"blue-service\""),
                        containsString("\"ServiceAddress\": \"145.123.145.157\""),
                        containsString("\"ServiceID\": \"blue-service::145.123.145.157::8080\""));

    }

}
