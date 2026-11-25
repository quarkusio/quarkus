package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;

@QuarkusTest
public class StaticResourceTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    @AfterEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    void staticResource() {
        given()
                .when()
                .get("/test.html")
                .then()
                .statusCode(200)
                .body(containsString("<html><body>Test</body></html>"));

        SpanDataList spans = spanExporter().fetch();
        assertTrue(spans.isEmpty());
    }
}
