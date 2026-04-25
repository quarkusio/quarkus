package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkus.it.opentelemetry.util.CustomExporter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OtlpCoexistenceIT.OtlpEnabledProfile.class)
public class OtlpCoexistenceIT {

    @Produces
    @Singleton
    CustomExporter customExporter = new CustomExporter();

    @Test
    void testCoexistence() {
        String response = given()
                .when()
                .get("/active-span-exporters")
                .then()
                .statusCode(200)
                .extract().asString();

        assertTrue(response.contains("InMemorySpanExporter"),
                "InMemory exporter should be registered");

        assertTrue(response.contains("CustomExporter"),
                "CustomExporter exporter should be registered");

        assertTrue(response.contains("LateBoundSpanProcessor"), "Default OTLP processor missing");
    }

    public static class OtlpEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.otel.experimental.otlp.default.enable", "true",
                    // Disable Metrics and Logs to isolate the Span (Trace) pipeline.
                    // Since the quarkus.otel.experimental.otlp.default.enable is true,
                    // we prevent 'AmbiguousResolution' conflicts for these signals
                    // during the integration test.
                    "quarkus.otel.metrics.exporter", "none",
                    "quarkus.otel.logs.exporter", "none");
        }
    }
}
