package org.acme;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LoggingMetricsNotPresentTest {

    @Test
    void testHelloEndpoint() throws InterruptedException {

        final TestingJulHandler julHandler = new TestingJulHandler();
        final Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.addHandler(julHandler);

        Thread.sleep(100);// give time to export metrics records (but must not be there)

        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello from Quarkus REST"));

        await().untilAsserted(() -> {
            assertThat(julHandler.logRecords).hasSizeGreaterThanOrEqualTo(1);
        });

        ArrayList<LogRecord> logRecords = julHandler.logRecords;
        rootLogger.removeHandler(julHandler);

        assertThat(logRecords.stream()
                .anyMatch(logRecord -> logRecord.getLoggerName().startsWith(GreetingResource.class.getName())))
                .as("Log line from the service must be logged")
                .isTrue();
        // Only present if opentelemetry-exporter-logging is used
        // But we are turning it off if metrics are disabled
        assertThat(logRecords.stream()
                .noneMatch(logRecord -> logRecord.getLoggerName()
                        .startsWith("io.opentelemetry.exporter.logging.LoggingMetricExporter")))
                .as("Log lines from the OTel logging metrics exporter must NOT be logged")
                .isTrue();
    }

    private static class TestingJulHandler extends Handler {

        private final ArrayList<LogRecord> logRecords = new ArrayList<>();

        public ArrayList<LogRecord> getLogRecords() {
            return logRecords;
        }

        @Override
        public void publish(LogRecord record) {
            logRecords.add(record);
        }

        @Override
        public void flush() {
            // Do nothing
        }

        @Override
        public void close() throws SecurityException {
            // Do nothing
        }
    }
}
