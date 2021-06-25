package io.quarkus.it.opentracing;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
public class OpenTracingTestCase {

    static GenericContainer postgreSQLContainer;

    @BeforeAll
    static void initPostgres() {
        postgreSQLContainer = new PostgreSQLContainer(PostgreSQLContainer.IMAGE)
                .withDatabaseName("mydatabase").withUsername("sa").withPassword("sa");
        postgreSQLContainer.setPortBindings(List.of("5432:5432"));
        postgreSQLContainer.start();
    }

    private void resetExporter() {
        given()
                .when().get("/export/clear")
                .then()
                .statusCode(204);
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<List<Map<String, Object>>>() {
        });
    }

    @Test
    void testResourceTracing() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));
        Assertions.assertNotNull(spanData.get("traceId"));
        Assertions.assertEquals("GET:io.quarkus.it.opentracing.SimpleResource.directTrace", spanData.get("operation_name"));
        Assertions.assertEquals(0, spanData.get("parent_spanId"));

        Assertions.assertEquals("server", spanData.get("tag_span.kind"));
        Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
        Assertions.assertEquals("GET", spanData.get("tag_http.method"));
        Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
    }

    @Test
    void testChainedResourceTracing() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/chained")
                .then()
                .statusCode(200)
                .body("message", equalTo("Chained trace"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));
        Assertions.assertNotNull(spanData.get("traceId"));
        Assertions.assertEquals("GET:io.quarkus.it.opentracing.SimpleResource.chainedTrace", spanData.get("operation_name"));
        Assertions.assertEquals(0, spanData.get("parent_spanId"));

        Assertions.assertEquals("server", spanData.get("tag_span.kind"));
        Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
        Assertions.assertEquals("GET", spanData.get("tag_http.method"));
        Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
    }

    @Test
    void testDeepPathNaming() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/deep/path")
                .then()
                .statusCode(200)
                .body("message", equalTo("Deep url path"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));
        Assertions.assertNotNull(spanData.get("traceId"));
        Assertions.assertEquals("GET:io.quarkus.it.opentracing.SimpleResource.deepUrlPathTrace",
                spanData.get("operation_name"));
        Assertions.assertEquals(0, spanData.get("parent_spanId"));

        Assertions.assertEquals("server", spanData.get("tag_span.kind"));
        Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
        Assertions.assertEquals("GET", spanData.get("tag_http.method"));
        Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
    }

    @Test
    void testPathParameter() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/param/12345")
                .then()
                .statusCode(200)
                .body("message", equalTo("ParameterId: 12345"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));
        Assertions.assertNotNull(spanData.get("traceId"));
        Assertions.assertEquals("GET:io.quarkus.it.opentracing.SimpleResource.pathParameters", spanData.get("operation_name"));
        Assertions.assertEquals(0, spanData.get("parent_spanId"));

        Assertions.assertEquals("server", spanData.get("tag_span.kind"));
        Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
        Assertions.assertEquals("GET", spanData.get("tag_http.method"));
        Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
    }

    @Test
    void testClientTracing() {
        resetExporter();

        given()
                .when().get("/client/ping/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpans().size() == 3);

        boolean outsideServerFound = false;
        boolean clientFound = false;

        for (Map<String, Object> spanData : getSpans()) {
            Assertions.assertNotNull(spanData);
            Assertions.assertNotNull(spanData.get("spanId"));

            if (spanData.get("tag_span.kind").equals("server")
                    && spanData.get("operation_name").equals("GET:io.quarkus.it.opentracing.PingPongResource.ping")) {
                outsideServerFound = true;
                // Server Span
                Assertions.assertNotNull(spanData);
                Assertions.assertNotNull(spanData.get("spanId"));
                Assertions.assertNotNull(spanData.get("traceId"));
                Assertions.assertEquals("GET:io.quarkus.it.opentracing.PingPongResource.ping",
                        spanData.get("operation_name"));
                Assertions.assertEquals(0, spanData.get("parent_spanId"));

                Assertions.assertEquals("server", spanData.get("tag_span.kind"));
                Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
                Assertions.assertEquals("GET", spanData.get("tag_http.method"));
                Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
            } else if (spanData.get("tag_span.kind").equals("server")
                    && spanData.get("operation_name").equals("GET:io.quarkus.it.opentracing.PingPongResource.pong")) {
                clientFound = true;
                // Client span
                Assertions.assertNotNull(spanData);
                Assertions.assertNotNull(spanData.get("spanId"));
                Assertions.assertNotNull(spanData.get("traceId"));
                Assertions.assertEquals("GET:io.quarkus.it.opentracing.PingPongResource.pong",
                        spanData.get("operation_name"));
                Assertions.assertEquals("server", spanData.get("tag_span.kind"));
                Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
                Assertions.assertEquals("GET", spanData.get("tag_http.method"));
                Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
            }
        }

        Assertions.assertTrue(outsideServerFound);
        Assertions.assertTrue(clientFound);
    }

    @Test
    void testAsyncClientTracing() {
        resetExporter();

        given()
                .when().get("/client/async-ping/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpans().size() == 3);

        boolean outsideServerFound = false;
        boolean clientFound = false;

        for (Map<String, Object> spanData : getSpans()) {
            Assertions.assertNotNull(spanData);
            Assertions.assertNotNull(spanData.get("spanId"));

            if (spanData.get("tag_span.kind").equals("server")
                    && spanData.get("operation_name").equals("GET:io.quarkus.it.opentracing.PingPongResource.asyncPing")) {
                outsideServerFound = true;
                // Server Span
                Assertions.assertNotNull(spanData);
                Assertions.assertNotNull(spanData.get("spanId"));
                Assertions.assertNotNull(spanData.get("traceId"));
                Assertions.assertEquals("GET:io.quarkus.it.opentracing.PingPongResource.asyncPing",
                        spanData.get("operation_name"));
                Assertions.assertEquals(0, spanData.get("parent_spanId"));

                Assertions.assertEquals("server", spanData.get("tag_span.kind"));
                Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
                Assertions.assertEquals("GET", spanData.get("tag_http.method"));
                Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
            } else if (spanData.get("tag_span.kind").equals("server")
                    && spanData.get("operation_name").equals("GET:io.quarkus.it.opentracing.PingPongResource.pong")) {
                clientFound = true;
                // Client span
                Assertions.assertNotNull(spanData);
                Assertions.assertNotNull(spanData.get("spanId"));
                Assertions.assertNotNull(spanData.get("traceId"));
                Assertions.assertEquals("GET:io.quarkus.it.opentracing.PingPongResource.pong",
                        spanData.get("operation_name"));
                Assertions.assertEquals("server", spanData.get("tag_span.kind"));
                Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
                Assertions.assertEquals("GET", spanData.get("tag_http.method"));
                Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
            }
        }

        Assertions.assertTrue(outsideServerFound);
        Assertions.assertTrue(clientFound);
    }

    @Test
    void testJdbcTracing() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/jdbc")
                .then()
                .statusCode(200)
                .body("message", equalTo("1"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpans().size() == 3);

        boolean resourceSpanFound = false;
        boolean jdbcSpanFound = false;
        for (Map<String, Object> spanData : getSpans()) {
            Assertions.assertNotNull(spanData);
            Assertions.assertNotNull(spanData.get("spanId"));
            Assertions.assertNotNull(spanData.get("traceId"));

            if (spanData.get("operation_name").equals("GET:io.quarkus.it.opentracing.JdbcResource.jdbc")) {
                Assertions.assertEquals(0, spanData.get("parent_spanId"));
                Assertions.assertEquals("server", spanData.get("tag_span.kind"));
                Assertions.assertEquals("jaxrs", spanData.get("tag_component"));
                Assertions.assertEquals("GET", spanData.get("tag_http.method"));
                Assertions.assertEquals("200", spanData.get("tag_http.status_code"));
                resourceSpanFound = true;
            } else if (spanData.get("operation_name").equals("Query")) {
                Assertions.assertEquals("client", spanData.get("tag_span.kind"));
                Assertions.assertEquals("java-jdbc", spanData.get("tag_component"));
                Assertions.assertEquals("select 1", spanData.get("tag_db.statement"));
                jdbcSpanFound = true;
            }
        }
        Assertions.assertTrue(resourceSpanFound);
        Assertions.assertTrue(jdbcSpanFound);
    }
}
