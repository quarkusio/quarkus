package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;

@QuarkusTest
@TestProfile(QuarkusTestProfileTest.TestProfile.class)
public class QuarkusTestProfileTest {
    @Test
    void testProfile() {
        given()
                .get("/config/{name}", "test.profile.main")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("main"));

        given()
                .get("/config/{name}", "test.profile.custom")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("custom"));
    }

    @Inject
    VertxHttpBuildTimeConfig vertxHttpBuildTimeConfig;

    @Test
    void profiledBuildTimeRuntimeFixed() {
        assertTrue(vertxHttpBuildTimeConfig.enableCompression());
    }

    public static class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> configs = new HashMap<>();
            configs.put("quarkus.config.locations", "test-profile.properties");
            configs.put("smallrye.config.locations", "relocate.properties");
            configs.put("%custom.quarkus.http.enable-compression", "true");
            return configs;
        }

        @Override
        public String getConfigProfile() {
            return "custom";
        }
    }
}
