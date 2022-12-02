package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(FallbackLocationsTest.TestProfile.class)
public class FallbackLocationsTest {
    @Test
    void fallback() {
        given()
                .get("/config/{name}", "quarkus.config.locations")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("fallback.properties"));

        given()
                .get("/config/{name}", "locations.fallback")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"));
    }

    public static class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> configs = new HashMap<>();
            configs.put("smallrye.config.locations", "fallback.properties");
            return configs;
        }

        @Override
        public String getConfigProfile() {
            return "fallback";
        }
    }
}
