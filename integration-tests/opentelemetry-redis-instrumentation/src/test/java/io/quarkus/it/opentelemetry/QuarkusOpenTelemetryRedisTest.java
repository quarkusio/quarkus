package io.quarkus.it.opentelemetry;

import static io.opentelemetry.semconv.SemanticAttributes.DB_CONNECTION_STRING;
import static io.opentelemetry.semconv.SemanticAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.SemanticAttributes.DB_REDIS_DATABASE_INDEX;
import static io.opentelemetry.semconv.SemanticAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.SemanticAttributes.DbSystemValues.REDIS;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.vertx.mutiny.redis.client.Command;

@QuarkusTest
@SuppressWarnings("unchecked")
class QuarkusOpenTelemetryRedisTest {
    static final String SYNC_KEY = "sync-key";
    static final String SYNC_VALUE = "sync-value";
    static final String REACTIVE_KEY = "reactive-key";
    static final String REACTIVE_VALUE = "reactive-value";
    static final String INVALID_OPERATION_PATH = "invalid-operation";
    static final String CONNECTION_STRING = "localhost:16379";

    String getKey(String k) {
        return k;
    }

    @BeforeEach
    void reset() {
        given().get("/opentelemetry/reset").then().statusCode(HTTP_OK);
        await().atMost(5, SECONDS).until(() -> getSpans().isEmpty());
    }

    @Test
    public void syncValidOperation() {
        String path = String.format("redis/sync/%s", getKey(SYNC_KEY));
        String getCommand = Command.GET.toString();
        String setCommand = Command.SET.toString();

        RestAssured.given()
                .body(SYNC_VALUE)
                .when()
                .post(path)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get(path)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(SYNC_VALUE));

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> getSpans().size() == 4);

        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> setSpan = findSpan(spans,
                m -> SpanKind.CLIENT.name().equals(m.get("kind")) && setCommand.equals(m.get("name")));
        Map<String, Object> getSpan = findSpan(spans,
                m -> SpanKind.CLIENT.name().equals(m.get("kind")) && getCommand.equals(m.get("name")));

        Map<String, Object> setAttributes = (Map<String, Object>) setSpan.get("attributes");
        Map<String, Object> getAttributes = (Map<String, Object>) getSpan.get("attributes");

        // SET
        assertEquals(setCommand, setSpan.get("name"));
        assertEquals(setCommand, setAttributes.get(DB_OPERATION.getKey()));
        assertEquals(REDIS, setAttributes.get(DB_SYSTEM.getKey()));
        assertEquals(CONNECTION_STRING, setAttributes.get(DB_CONNECTION_STRING.getKey()));
        assertEquals(0, setAttributes.get(DB_REDIS_DATABASE_INDEX.getKey()));
        // GET
        assertEquals(getCommand, getSpan.get("name"));
        assertEquals(getCommand, getAttributes.get(DB_OPERATION.getKey()));
        assertEquals(REDIS, getAttributes.get(DB_SYSTEM.getKey()));
        assertEquals(CONNECTION_STRING, getAttributes.get(DB_CONNECTION_STRING.getKey()));
        assertEquals(0, getAttributes.get(DB_REDIS_DATABASE_INDEX.getKey()));
    }

    @Test
    public void syncInvalidOperation() {
        String path = String.format("redis/sync/%s", getKey(INVALID_OPERATION_PATH));

        RestAssured.post(path)
                .then()
                .statusCode(500);

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> getSpans().size() == 2);

        Map<String, Object> span = findSpan(getSpans(), m -> SpanKind.CLIENT.name().equals(m.get("kind")));

        Map<String, Object> status = (Map<String, Object>) span.get("status");
        Map<String, Object> event = ((List<Map<String, Object>>) span.get("events")).get(0);
        Map<String, Object> exception = (Map<String, Object>) event.get("exception");

        assertEquals("bazinga", span.get("name"));
        assertEquals("ERROR", status.get("statusCode"));
        assertEquals("exception", event.get("name"));

        checkForException(exception);

    }

    void checkForException(Map<String, Object> exception) {
        assertThat((String) exception.get("message"), containsString("ERR unknown command 'bazinga'"));
    }

    @Test
    public void reactiveValidOperation() {
        String path = String.format("redis/reactive/%s", getKey(REACTIVE_KEY));
        String getCommand = Command.GET.toString();
        String setCommand = Command.SET.toString();

        RestAssured.given()
                .body(REACTIVE_VALUE)
                .when()
                .post(path)
                .then()
                .statusCode(204);

        RestAssured.given()
                .when()
                .get(path)
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(REACTIVE_VALUE));

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> getSpans().size() == 4);

        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> setSpan = findSpan(spans,
                m -> SpanKind.CLIENT.name().equals(m.get("kind")) && setCommand.equals(m.get("name")));
        Map<String, Object> getSpan = findSpan(spans,
                m -> SpanKind.CLIENT.name().equals(m.get("kind")) && getCommand.equals(m.get("name")));

        Map<String, Object> setAttributes = (Map<String, Object>) setSpan.get("attributes");
        Map<String, Object> getAttributes = (Map<String, Object>) getSpan.get("attributes");

        // SET
        assertEquals(setCommand, setSpan.get("name"));
        assertEquals(setCommand, setAttributes.get(DB_OPERATION.getKey()));
        assertEquals(REDIS, setAttributes.get(DB_SYSTEM.getKey()));
        assertEquals(CONNECTION_STRING, setAttributes.get(DB_CONNECTION_STRING.getKey()));
        assertEquals(0, setAttributes.get(DB_REDIS_DATABASE_INDEX.getKey()));
        // GET
        assertEquals(getCommand, getSpan.get("name"));
        assertEquals(getCommand, getAttributes.get(DB_OPERATION.getKey()));
        assertEquals(REDIS, getAttributes.get(DB_SYSTEM.getKey()));
        assertEquals(CONNECTION_STRING, getAttributes.get(DB_CONNECTION_STRING.getKey()));
        assertEquals(0, getAttributes.get(DB_REDIS_DATABASE_INDEX.getKey()));
    }

    @Test
    public void reactiveInvalidOperation() {
        String path = String.format("redis/reactive/%s", getKey(INVALID_OPERATION_PATH));

        RestAssured.post(path)
                .then()
                .statusCode(500);

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> getSpans().size() == 2);

        Map<String, Object> span = findSpan(getSpans(), m -> SpanKind.CLIENT.name().equals(m.get("kind")));

        Map<String, Object> status = (Map<String, Object>) span.get("status");
        Map<String, Object> event = ((List<Map<String, Object>>) span.get("events")).get(0);
        Map<String, Object> exception = (Map<String, Object>) event.get("exception");

        assertEquals("bazinga", span.get("name"));
        assertEquals("ERROR", status.get("statusCode"));
        assertEquals("exception", event.get("name"));

        checkForException(exception);
    }

    @Test
    public void taintedConnection() {
        RestAssured.get("redis/tainted")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("OK"));

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> getSpans().size() == 3);

        Map<String, Object> span = findSpan(getSpans(), m -> SpanKind.CLIENT.name().equals(m.get("kind"))
                && "get".equals(m.get("name")));
        Map<String, Object> attributes = (Map<String, Object>) span.get("attributes");

        assertEquals("redis", attributes.get("db.system"));
        assertEquals("get", attributes.get("db.operation"));
        assertFalse(span.containsKey("db.redis.database_index"));
    }

    private List<Map<String, Object>> getSpans() {
        return get("/opentelemetry/export").body().as(new TypeRef<>() {
        });
    }

    private static Map<String, Object> findSpan(List<Map<String, Object>> spans,
            Predicate<Map<String, Object>> spanDataSelector) {
        Optional<Map<String, Object>> select = spans.stream().filter(spanDataSelector).findFirst();
        Assertions.assertTrue(select.isPresent());
        Map<String, Object> spanData = select.get();
        Assertions.assertNotNull(spanData.get("spanId"));
        return spanData;
    }
}
