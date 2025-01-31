package io.quarkus.it.opentelemetry.vertx;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.DB_CONNECTION_STRING;
import static io.opentelemetry.semconv.SemanticAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.SemanticAttributes.DB_STATEMENT;
import static io.opentelemetry.semconv.SemanticAttributes.DB_USER;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.ext.web.Router;
import io.vertx.mutiny.sqlclient.Pool;

@QuarkusTest
public class SqlClientTest {
    @Inject
    Router router;

    @Inject
    Pool pool;

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @Test
    void sqlClient() {
        pool.query("CREATE TABLE IF NOT EXISTS USERS (id INT, name VARCHAR(100));")
                .execute().await().indefinitely();

        router.get("/sqlClient").handler(rc -> {
            pool
                    .query("SELECT * FROM USERS")
                    .execute()
                    .subscribe().with(event -> rc.response().end());

        });

        given()
                .get("/sqlClient")
                .then()
                .statusCode(HTTP_OK);

        // Why 3 spans:
        // Table creation + HTTP + Query = 3

        await().atMost(5, TimeUnit.SECONDS).until(() -> inMemorySpanExporter.getFinishedSpanItems().size() == 3);
        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();
        assertEquals(3, spans.size());

        // We cannot rely on the order, we must identify the spans.
        SpanData tableCreation = inMemorySpanExporter.getFinishedSpanItems().stream()
                .filter(sd -> sd.getName().contains("CREATE TABLE USERS")).findFirst().orElseThrow();
        SpanData httpSpan = inMemorySpanExporter.getFinishedSpanItems().stream()
                .filter(sd -> sd.getName().contains("GET /sqlClient")).findFirst().orElseThrow();
        SpanData querySpan = inMemorySpanExporter.getFinishedSpanItems().stream()
                .filter(sd -> sd.getName().contains("SELECT USERS")).findFirst().orElseThrow();

        assertNotEquals(httpSpan.getTraceId(), tableCreation.getTraceId()); // No relationship
        assertEquals(httpSpan.getTraceId(), querySpan.getTraceId());
        assertEquals(httpSpan.getSpanId(), querySpan.getParentSpanId());

        assertEquals("GET /sqlClient", httpSpan.getName());
        assertEquals(HTTP_OK, httpSpan.getAttributes().get(HTTP_RESPONSE_STATUS_CODE));

        assertEquals("SELECT USERS", querySpan.getName());
        assertEquals(CLIENT, querySpan.getKind());
        assertEquals("SELECT", querySpan.getAttributes().get(DB_OPERATION));
        assertEquals("SELECT * FROM USERS", querySpan.getAttributes().get(DB_STATEMENT));
        assertEquals("quarkus", querySpan.getAttributes().get(DB_USER));
        assertNotNull(querySpan.getAttributes().get(DB_CONNECTION_STRING));

        assertEquals("CREATE TABLE IF NOT EXISTS USERS (id INT, name VARCHAR(?));",
                tableCreation.getAttributes().get(DB_STATEMENT));
        assertEquals("quarkus", tableCreation.getAttributes().get(DB_USER));
    }

    @BeforeEach
    @AfterEach
    void reset() {
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            // make sure spans from previous tests are not included
            List<SpanData> finishedSpanItems = inMemorySpanExporter.getFinishedSpanItems();
            if (finishedSpanItems.size() > 0) {
                inMemorySpanExporter.reset();
            }
            return finishedSpanItems.size() == 0;
        });
    }
}
