package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.given;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanExporterClient;

@QuarkusTest
public class OpenTelemetryInjectionsTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    @AfterEach
    void reset() {
        // make sure spans are cleared
        spanExporter().reset();
    }

    @Test
    public void testOTelInjections() {
        given()
                .when().get("/otel/injection")
                .then()
                .statusCode(200);
        spanExporter().awaitCount(1, Duration.ofSeconds(10));
    }

    @Test
    public void testOTelInjectionsAsync() {
        given()
                .when().get("/otel/injection/async")
                .then()
                .statusCode(200);
        spanExporter().awaitCount(1, Duration.ofSeconds(10));
    }
}
