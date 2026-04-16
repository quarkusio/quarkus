package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OtlpCoexistenceIT.OtlpEnabledProfile.class)
public class OtlpCoexistenceIT {

    @Test
    void testCoexistence() {
        String response = given()
                .when()
                .get("/export/count")
                .then()
                .statusCode(200)
                .extract().asString();

        assertTrue(response.contains("InMemorySpanExporter"),
                "InMemory exporter should be registered");

        String[] exporters = response.split(",");
        assertTrue(exporters.length >= 2,
                "Should have at least 2 exporters registered, but found: " + response);
    }

    public static class OtlpEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.otel.experimental.otlp.default.enable", "true");
        }
    }
}
