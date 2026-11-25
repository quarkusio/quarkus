package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;

public abstract class OpenTelemetryJdbcInstrumentationTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    private static final AttributeKey<String> DB_SYSTEM = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");
    private static final AttributeKey<String> DB_SQL_TABLE = AttributeKey.stringKey("db.sql.table");

    @BeforeEach
    @AfterEach
    void reset() {
        // make sure spans are cleared
        spanExporter().reset();
    }

    protected void testQueryTraced(String dbKind, String expectedTable) {
        given()
                .queryParam("id", 1)
                .when().post("/hit/" + dbKind)
                .then()
                .statusCode(200)
                .body("message", Matchers.equalTo("Hit message."));

        Awaitility.await().atMost(Duration.ofSeconds(55)).untilAsserted(() -> {
            assertFalse(spanExporter().fetch().isEmpty());

            // Assert insert has been traced
            boolean hitInserted = false;
            for (SpanData span : spanExporter().fetch()) {
                var dbOperation = span.getAttributes().get(DB_OPERATION);
                var dbTable = span.getAttributes().get(DB_SQL_TABLE);
                if ("INSERT".equals(dbOperation) && expectedTable.equals(dbTable)) {
                    hitInserted = true;
                    break;
                }
            }
            assertTrue(hitInserted, "JDBC insert statement was not traced.");
        });
    }

    protected void testQueryNotTraced(String dbKind) {
        given()
                .queryParam("id", 1)
                .when().post("/hit/" + dbKind)
                .then()
                .statusCode(200)
                .body("message", Matchers.equalTo("Hit message."));

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertTrue(spanExporter().fetch().isEmpty(), "No spans should be recorded when OpenTelemetry is disabled.");
        });
    }

    protected void testConnectionNotTraced(String dbKind) {
        given()
                .queryParam("id", 2)
                .when().post("/hit/" + dbKind)
                .then()
                .statusCode(200)
                .body("message", Matchers.equalTo("Hit message."));

        Awaitility.await().atMost(Duration.ofSeconds(55)).untilAsserted(() -> {
            SpanDataList spans = spanExporter().fetch();
            assertFalse(spans.isEmpty());

            // Verify no connection acquisition span is present (only statement spans with db.operation)
            for (SpanData span : spans) {
                if (isConnectionSpan(span)) {
                    throw new AssertionError(
                            "Connection acquisition span should not be present when trace-connection is disabled. "
                                    + "SpanData attributes: " + span.getAttributes());
                }
            }
        });
    }

    protected void testConnectionTraced(String dbKind) {
        given()
                .queryParam("id", 1)
                .when().post("/hit/" + dbKind)
                .then()
                .statusCode(200)
                .body("message", Matchers.equalTo("Hit message."));

        Awaitility.await().atMost(Duration.ofSeconds(55)).untilAsserted(() -> {
            SpanDataList spans = spanExporter().fetch();
            assertFalse(spans.isEmpty());

            // Verify at least one connection acquisition span IS present
            // (has db.system but no db.operation)
            boolean connectionSpanFound = false;
            for (SpanData span : spans) {
                if (isConnectionSpan(span)) {
                    connectionSpanFound = true;
                    break;
                }
            }
            assertTrue(connectionSpanFound,
                    "Connection acquisition span should be present when trace-connection is enabled.");
        });
    }

    private static boolean isConnectionSpan(SpanData span) {
        Set<String> names = span.getAttributes().asMap().keySet().stream().map(AttributeKey::getKey).collect(toSet());
        return names.contains(DB_SYSTEM.getKey()) && !names.contains(DB_OPERATION.getKey());
    }
}
