package io.quarkus.it.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.DbAttributes.DB_NAMESPACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_LOCAL_ADDRESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_LOCAL_PORT;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_ADDRESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_PORT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_REDIS_DATABASE_INDEX;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DbSystemNameIncubatingValues.REDIS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.net.URI;
import java.time.Duration;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.RestAssured;
import io.vertx.redis.client.Command;

@QuarkusTest
class QuarkusOpenTelemetryRedisTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    static final String SYNC_KEY = "sync-key";
    static final String SYNC_VALUE = "sync-value";
    static final String REACTIVE_KEY = "reactive-key";
    static final String REACTIVE_VALUE = "reactive-value";
    static final String INVALID_OPERATION_PATH = "invalid-operation";

    String getKey(String k) {
        return k;
    }

    @BeforeEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    public void syncValidOperation() {
        String path = String.format("redis/sync/%s", getKey(SYNC_KEY));
        String getCommand = Command.GET.toString() + " 0";
        String setCommand = Command.SET.toString() + " 0";

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

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofSeconds(10));

        SpanData setSpan = spans.ofKind(SpanKind.CLIENT).named(setCommand).single();
        SpanData getSpan = spans.ofKind(SpanKind.CLIENT).named(getCommand).single();

        // SET
        assertThat(setSpan)
                .hasName(setCommand)
                .hasAttribute(DB_OPERATION, "set")
                .hasAttribute(DB_SYSTEM, REDIS)
                .hasAttribute(DB_NAMESPACE, "0")
                .hasAttribute(NETWORK_PEER_ADDRESS, "127.0.0.1")
                .hasAttribute(NETWORK_PEER_PORT, 16379L)
                .hasAttribute(NETWORK_LOCAL_ADDRESS, "localhost")
                .hasAttribute(NETWORK_LOCAL_PORT, 16379L);
        // GET
        assertThat(getSpan)
                .hasName(getCommand)
                .hasAttribute(DB_OPERATION, "get")
                .hasAttribute(DB_SYSTEM, REDIS);
        assertThat(setSpan)
                .hasAttribute(DB_NAMESPACE, "0")
                .hasAttribute(NETWORK_PEER_ADDRESS, "127.0.0.1")
                .hasAttribute(NETWORK_PEER_PORT, 16379L)
                .hasAttribute(NETWORK_LOCAL_ADDRESS, "localhost")
                .hasAttribute(NETWORK_LOCAL_PORT, 16379L);
    }

    @Test
    public void syncInvalidOperation() {
        String path = String.format("redis/sync/%s", getKey(INVALID_OPERATION_PATH));

        RestAssured.post(path)
                .then()
                .statusCode(500);

        SpanData span = spanExporter().awaitCount(2, Duration.ofSeconds(10)).ofKind(SpanKind.CLIENT).single();

        EventData event = span.getEvents().getFirst();

        assertThat(span)
                .hasName("bazinga 0")
                .hasStatusSatisfying(status -> status.hasCode(StatusCode.ERROR));
        assertEquals("exception", event.getName());

        checkForException(event);

    }

    private static void checkForException(EventData exception) {
        ExceptionEventData recorded = assertInstanceOf(ExceptionEventData.class, exception);
        assertThat(recorded.getException().getMessage(), containsString("ERR unknown command 'bazinga'"));
        assertThat(exception.getAttributes().get(EXCEPTION_MESSAGE), containsString("ERR unknown command 'bazinga'"));
    }

    @Test
    public void reactiveValidOperation() {
        String path = String.format("redis/reactive/%s", getKey(REACTIVE_KEY));
        String getCommand = Command.GET.toString() + " 0";
        String setCommand = Command.SET.toString() + " 0";

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

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofSeconds(10));

        SpanData setSpan = spans.ofKind(SpanKind.CLIENT).named(setCommand).single();
        SpanData getSpan = spans.ofKind(SpanKind.CLIENT).named(getCommand).single();

        // SET
        assertThat(setSpan)
                .hasName(setCommand)
                .hasAttribute(DB_OPERATION, "set")
                .hasAttribute(DB_SYSTEM, REDIS)
                .hasAttribute(DB_NAMESPACE, "0")
                .hasAttribute(NETWORK_PEER_ADDRESS, "127.0.0.1")
                .hasAttribute(NETWORK_PEER_PORT, 16379L)
                .hasAttribute(NETWORK_LOCAL_ADDRESS, "localhost")
                .hasAttribute(NETWORK_LOCAL_PORT, 16379L);
        // GET
        assertThat(getSpan)
                .hasName(getCommand)
                .hasAttribute(DB_OPERATION, "get")
                .hasAttribute(DB_SYSTEM, REDIS)
                .hasAttribute(DB_NAMESPACE, "0");
        assertThat(setSpan)
                .hasAttribute(NETWORK_PEER_ADDRESS, "127.0.0.1")
                .hasAttribute(NETWORK_PEER_PORT, 16379L)
                .hasAttribute(NETWORK_LOCAL_ADDRESS, "localhost")
                .hasAttribute(NETWORK_LOCAL_PORT, 16379L);
    }

    @Test
    public void reactiveInvalidOperation() {
        String path = String.format("redis/reactive/%s", getKey(INVALID_OPERATION_PATH));

        RestAssured.post(path)
                .then()
                .statusCode(500);

        SpanData span = spanExporter().awaitCount(2, Duration.ofSeconds(10)).ofKind(SpanKind.CLIENT).single();

        EventData event = span.getEvents().getFirst();

        assertThat(span)
                .hasName("bazinga 0")
                .hasStatusSatisfying(status -> status.hasCode(StatusCode.ERROR));
        assertEquals("exception", event.getName());

        checkForException(event);
    }

    @Test
    public void taintedConnection() {
        RestAssured.get("redis/tainted")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("OK"));

        SpanData span = spanExporter().awaitCount(3, Duration.ofSeconds(10)).ofKind(SpanKind.CLIENT).named("get").single();

        assertThat(span)
                .hasAttribute(DB_SYSTEM, "redis")
                .hasAttribute(DB_OPERATION, "get");
        assertThat(span.getAttributes()).doesNotContainKey(DB_REDIS_DATABASE_INDEX.getKey());
    }
}
