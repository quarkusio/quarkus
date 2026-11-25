package io.quarkus.it.opentelemetry.vertx;

import static io.opentelemetry.api.trace.SpanKind.CONSUMER;
import static io.opentelemetry.api.trace.SpanKind.PRODUCER;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_REQUEST_BODY_SIZE;
import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_RESPONSE_BODY_SIZE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.http.ContentType;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
class HelloRouterTest extends SpanExporterBaseTest {

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
    void span() {
        given()
                .get("/hello")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello"));

        SpanDataList spans = spanExporter().awaitCount(1, Duration.ofSeconds(10));

        assertThat(spans.ofKind(SERVER).single())
                .hasName("GET /hello")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.GET.toString())
                .hasAttribute(HTTP_ROUTE, "/hello");
    }

    @Test
    void spanPath() {
        given()
                .get("/hello/{name}", "Naruto")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello Naruto"));

        SpanDataList spans = spanExporter().awaitCount(1, Duration.ofSeconds(10));

        assertThat(spans.ofKind(SERVER).single())
                .hasName("GET /hello/:name")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.GET.toString())
                .hasAttribute(HTTP_ROUTE, "/hello/:name");
    }

    @Test
    void post() {
        given()
                .contentType(ContentType.TEXT)
                .body("Naruto")
                .post("/hello/")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello Naruto"));

        SpanDataList spans = spanExporter().awaitCount(1, Duration.ofSeconds(10));

        assertThat(spans.ofKind(SERVER).single())
                .hasName("POST /hello")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.POST.toString())
                .hasAttribute(HTTP_ROUTE, "/hello")
                .hasAttribute(HTTP_REQUEST_BODY_SIZE, 6L)
                .hasAttribute(HTTP_RESPONSE_BODY_SIZE, 12L);
    }

    @Test
    void bus() {
        given()
                .get("/bus")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getMessages().size() == 1);

        List<String> messages = getMessages();
        assertEquals(1, messages.size());
        assertEquals("hello to bus", messages.get(0));

        // the requests to /bus/messages above are traced too, leave them out
        SpanDataList spans = spanExporter().awaitUntil("exactly 3 spans", Duration.ofSeconds(50),
                s -> withoutMessagesRequests(s).size() == 3);
        spans = withoutMessagesRequests(spans);
        assertThat(spans).hasSize(3);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        assertThat(server)
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.GET.toString())
                .hasAttribute(HTTP_ROUTE, "/bus");

        SpanData producer = spans.ofKind(PRODUCER).childrenOf(server).single();
        assertThat(producer)
                .hasAttribute(MESSAGING_SYSTEM, "vert.x")
                .hasAttribute(MESSAGING_DESTINATION_NAME, "bus");

        SpanData consumer = spans.ofKind(CONSUMER).childrenOf(producer).single();
        assertThat(consumer)
                .hasAttribute(MESSAGING_SYSTEM, "vert.x")
                .hasAttribute(MESSAGING_DESTINATION_NAME, "bus")
                .hasAttribute(MESSAGING_OPERATION, MessageOperation.RECEIVE.toString().toLowerCase(Locale.ROOT));
    }

    private static SpanDataList withoutMessagesRequests(SpanDataList spans) {
        return spans.filter(span -> !span.getName().contains("bus/messages"));
    }

}
