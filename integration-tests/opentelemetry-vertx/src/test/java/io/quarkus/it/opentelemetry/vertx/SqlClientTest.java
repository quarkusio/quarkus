package io.quarkus.it.opentelemetry.vertx;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_CONNECTION_STRING;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_USER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.spi.DataSourceProvider;
import io.vertx.ext.web.Router;
import io.vertx.jdbcclient.JDBCPool;

// H2 is not supported in native mode
@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class SqlClientTest {
    @Inject
    Router router;
    @Inject
    Vertx vertx;
    @Inject
    DataSource dataSource;
    @Inject
    DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig;
    @Inject
    DataSourcesRuntimeConfig dataSourcesRuntimeConfig;
    @Inject
    DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig;

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @Test
    void sqlClient() {
        router.get("/sqlClient").handler(rc -> {
            JsonObject config = new JsonObject()
                    .put("jdbcUrl", dataSourcesJdbcRuntimeConfig.jdbc.url.orElse(""))
                    .put("username", dataSourcesRuntimeConfig.defaultDataSource.username.orElse(""))
                    .put("database", dataSourcesBuildTimeConfig.defaultDataSource.dbKind.orElse(""));

            JDBCPool pool = JDBCPool.pool(vertx, DataSourceProvider.create(dataSource, config));
            pool
                    .query("SELECT * FROM USERS")
                    .execute()
                    .onSuccess(event -> {
                    })
                    .onFailure(event -> {
                    })
                    .compose(rows -> pool.close())
                    // onComplete is executed before the end of Sql Telemetry data. This causes warnings in Scope.close
                    .onComplete(event -> rc.response().end());

        });

        given()
                .get("/sqlClient")
                .then()
                .statusCode(HTTP_OK);

        await().atMost(5, TimeUnit.SECONDS).until(() -> inMemorySpanExporter.getFinishedSpanItems().size() == 2);
        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();
        assertEquals(2, spans.size());

        assertEquals(spans.get(0).getTraceId(), spans.get(1).getTraceId());
        assertEquals(spans.get(0).getSpanId(), spans.get(1).getParentSpanId());

        assertEquals("/sqlClient", spans.get(0).getName());
        assertEquals(HTTP_OK, spans.get(0).getAttributes().get(HTTP_STATUS_CODE));

        assertEquals("SELECT USERS", spans.get(1).getName());
        assertEquals(CLIENT, spans.get(1).getKind());
        assertEquals("h2", spans.get(1).getAttributes().get(DB_SYSTEM));
        assertEquals("SELECT", spans.get(1).getAttributes().get(DB_OPERATION));
        assertEquals("SELECT * FROM USERS", spans.get(1).getAttributes().get(DB_STATEMENT));
        assertEquals("quarkus", spans.get(1).getAttributes().get(DB_USER));
        assertNotNull(spans.get(1).getAttributes().get(DB_CONNECTION_STRING));
        //noinspection ConstantConditions
        assertTrue(spans.get(1).getAttributes().get(DB_CONNECTION_STRING).startsWith("jdbc:h2:tcp://localhost"));
    }

    @BeforeEach
    @AfterEach
    void reset() {
        inMemorySpanExporter.reset();
    }
}
