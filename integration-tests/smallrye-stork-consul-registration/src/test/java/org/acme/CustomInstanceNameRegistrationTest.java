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
 * Verifies that when {@code instance-name} is configured, the service is registered in Consul
 * using exactly that name as the service instance ID.
 */
@DisabledOnOs(OS.WINDOWS)
@QuarkusTest
@TestProfile(CustomInstanceNameRegistrationTest.CustomInstanceNameProfile.class)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class CustomInstanceNameRegistrationTest {

    static final String INSTANCE_NAME = "my-custom-instance-id";

    public static class CustomInstanceNameProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "minimal";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.stork.my-service.service-registrar.instance-name", INSTANCE_NAME);
        }
    }

    @Test
    public void testServiceRegisteredWithConfiguredInstanceName() {
        RestAssured.get("http://localhost:8500/v1/agent/service/" + INSTANCE_NAME)
                .then()
                .statusCode(200)
                .body(containsString("\"Service\": \"my-service\""),
                        containsString("\"ID\": \"" + INSTANCE_NAME + "\""));
    }
}