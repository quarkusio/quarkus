package io.quarkus.logging.opentelemetry.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LoggingResourceTest {
    @Inject
    InMemoryLogRecordExporter exporter;

    @Test
    public void testHelloEndpoint() {
        // This will create 1 log, but some logs could already exist.
        given()
                .when().get("/logging-opentelemetry/hello")
                .then()
                .statusCode(200)
                .body(is("Hello World"));

        // Wait for logs to be available as everything is async
        await().atMost(Duration.ofSeconds(10)).until(() -> hasLog(exporter, "Hello World"));

        LogRecordData item = exporter.getFinishedLogRecordItems().get(exporter.getFinishedLogRecordItems().size() - 1);
        assertEquals("Hello World", item.getBody().asString());
        assertEquals(Severity.INFO, item.getSeverity());
    }

    @Test
    public void testException() {
        // This will create 1 log, but some logs could already exist.
        given()
                .when().get("/logging-opentelemetry/exception")
                .then()
                .statusCode(200)
                .body(is("Oh no! An exception"));

        // Wait for logs to be available as everything is async
        await().atMost(Duration.ofSeconds(10)).until(() -> hasLog(exporter, "Oh no Exception!"));

        LogRecordData item = exporter.getFinishedLogRecordItems().get(exporter.getFinishedLogRecordItems().size() - 1);
        assertEquals("Oh no Exception!", item.getBody().asString());
        assertEquals(Severity.ERROR, item.getSeverity());
        assertEquals(1, item.getAttributes().size());
        assertTrue(item.getAttributes().get(AttributeKey.stringKey("thrown")).startsWith("""
                java.lang.RuntimeException: Exception!
                	at io.quarkus.logging.opentelemetry.it.LoggingResource.exception(LoggingResource.java:41)
                	at io.quarkus.logging.opentelemetry.it.LoggingResource_ClientProxy.exception(Unknown Source)"""));
    }

    private Boolean hasLog(InMemoryLogRecordExporter exporter, String body) {
        return exporter.getFinishedLogRecordItems() != null &&
                !exporter.getFinishedLogRecordItems().isEmpty() &&
                exporter.getFinishedLogRecordItems().get(exporter.getFinishedLogRecordItems().size() - 1).getBody().asString()
                        .equals(body);

    }
}
