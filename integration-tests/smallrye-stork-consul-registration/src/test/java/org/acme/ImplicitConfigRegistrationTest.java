package org.acme;

import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@DisabledOnOs(OS.WINDOWS)
@QuarkusTest
@TestProfile(ImplicitConfigRegistrationTest.ImplicitConfigProfile.class)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class ImplicitConfigRegistrationTest {

    public static class ImplicitConfigProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "implicit";
        }
    }

    @Test
    public void test() {
        RestAssured.get("http://localhost:8500/v1/agent/service/quarkus-integration-test-smallrye-stork-consul-registration")
                .then()
                .statusCode(200)
                .body(containsString("\"Service\": \"quarkus-integration-test-smallrye-stork-consul-registration\""));

    }

}
