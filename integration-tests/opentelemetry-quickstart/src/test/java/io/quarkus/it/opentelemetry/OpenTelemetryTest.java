package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanExporterClient;

@QuarkusTest
public class OpenTelemetryTest extends BaseTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    void buildTimeEnabled() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello from Quarkus REST"));
        spanExporter().awaitCount(1, Duration.ofSeconds(5));
    }
}
