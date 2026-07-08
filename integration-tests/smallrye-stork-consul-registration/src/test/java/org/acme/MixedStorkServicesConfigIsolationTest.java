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
@TestProfile(MixedStorkServicesConfigIsolationTest.MixedStorkConfigProfile.class)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
@Disabled("Require a Stork v3 update")
public class MixedStorkServicesConfigIsolationTest {

    public static class MixedStorkConfigProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "multiple-stork";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.stork.red-service.service-registrar.type", "consul",
                    "quarkus.stork.blue-service.service-registrar.type", "consul",
                    "quarkus.stork.blue-service.service-registrar.enabled", "false");
        }

    }

    @Test
    public void test() {
        RestAssured.get(ConsulTestUtils.serviceUrl("red-service"))
                .then()
                .statusCode(200)
                .body(containsString("\"ServiceName\": \"red-service\""))
                .body("ServiceID", hasItem(matchesPattern("^red-service::[0-9.]+::8080")));

        ConsulTestUtils.assertServiceNotRegistered("blue-service");

    }

}
