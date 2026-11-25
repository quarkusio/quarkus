package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.UrlAttributes.URL_FULL;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.runtime.tracing.security.SecurityEventUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;

@QuarkusTest
public class OpenTelemetryReactiveTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    @AfterEach
    void reset() {
        // make sure spans are cleared
        spanExporter().reset();
    }

    @Test
    void get() {
        given()
                .when()
                .queryParam("name", "Naruto")
                .get("/reactive")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto"));

        SpanDataList spans = spanExporter().awaitCount(2, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);
    }

    @Test
    void helloGetUniDelayTest() {
        given()
                .when()
                .get("/reactive/hello-get-uni-delay")
                .then()
                .statusCode(200)
                .body(equalTo("helloGetUniDelay"));

        SpanDataList spans = spanExporter().awaitCount(2, Duration.ofSeconds(10));
        SpanData parent = spans.ofKind(SERVER).roots().single();
        assertThat(parent).hasName("GET /reactive/hello-get-uni-delay");

        SpanData child = spans.ofKind(INTERNAL).childrenOf(parent).single();
        assertThat(child).hasName("helloGetUniDelay");

        assertThat(child).hasTraceId(parent.getTraceId());
    }

    @Test
    void helloGetUniExecutorTest() {
        given()
                .when()
                .get("/reactive/hello-get-uni-executor")
                .then()
                .statusCode(200)
                .body(equalTo("helloGetUniExecutor"));

        SpanDataList spans = spanExporter().awaitCount(2, Duration.ofSeconds(10));
        SpanData parent = spans.ofKind(SERVER).roots().single();
        assertThat(parent).hasName("GET /reactive/hello-get-uni-executor");

        SpanData child = spans.ofKind(INTERNAL).childrenOf(parent).single();
        assertThat(child).hasName("helloGetUniExecutor");

        assertThat(child).hasTraceId(parent.getTraceId());
    }

    @Test
    void blockingException() {
        given()
                .when()
                .get("/reactive/blockingException")
                .then()
                .statusCode(500);

        assertExceptionRecorded();
    }

    @Test
    void reactiveException() {
        given()
                .when()
                .get("/reactive/reactiveException")
                .then()
                .statusCode(500);

        assertExceptionRecorded();
    }

    private void assertExceptionRecorded() {
        SpanDataList spans = spanExporter().awaitUntil("exactly 1 exception event", Duration.ofSeconds(5),
                s -> exceptionEvents(s).size() == 1);
        assertThat(exceptionEvents(spans)).singleElement().satisfies(event -> {
            assertThat(event).hasName("exception");
            assertThat(event).isInstanceOfSatisfying(ExceptionEventData.class,
                    recorded -> assertThat(recorded.getException().getMessage()).contains("dummy"));
            assertThat(event.getAttributes().get(EXCEPTION_MESSAGE)).contains("dummy");
        });
    }

    private static List<EventData> exceptionEvents(SpanDataList spans) {
        return spans.stream().flatMap(span -> span.getEvents().stream())
                .filter(event -> "exception".equals(event.getName()) || event instanceof ExceptionEventData)
                .toList();
    }

    @Test
    void post() {
        given()
                .when()
                .body("Naruto")
                .post("/reactive")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto"));

        SpanDataList spans = spanExporter().awaitCount(2, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);
    }

    @Test
    void multipleUsingChain() {
        given()
                .when()
                .get("/reactive/multiple-chain")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto and Hello Goku"));

        SpanDataList spans = spanExporter().awaitCount(7, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        // First span is the call getting into the server. It does not have a parent span.
        SpanData parent = spans.ofKind(SERVER).roots().single();

        // We should get 2 client spans originated by the server
        SpanDataList clientSpans = spans.ofKind(CLIENT).childrenOf(parent);
        assertThat(clientSpans).hasSize(2);

        // Each client calls the server and programmatically create a span, so each have a server and an internal span

        // Naruto Span
        SpanData naruto = clientSpans.filter(span -> span.getAttributes().get(URL_FULL).contains("Naruto")).single();

        SpanData narutoServer = spans.ofKind(SERVER).childrenOf(naruto).single();
        assertThat(narutoServer)
                .hasAttribute(URL_PATH, "/reactive")
                .hasAttribute(URL_QUERY, "name=Naruto");
        SpanData narutoInternal = spans.ofKind(INTERNAL).childrenOf(narutoServer).single();
        assertThat(narutoInternal).hasName("helloGet");

        // Goku Span
        SpanData goku = clientSpans.filter(span -> span.getAttributes().get(URL_FULL).contains("Goku")).single();

        SpanData gokuServer = spans.ofKind(SERVER).childrenOf(goku).single();
        assertThat(gokuServer)
                .hasAttribute(URL_PATH, "/reactive")
                .hasAttribute(URL_QUERY, "name=Goku");
        SpanData gokuInternal = spans.ofKind(INTERNAL).childrenOf(gokuServer).single();
        assertThat(gokuInternal).hasName("helloGet");
    }

    @Test
    void multipleUsingCombine() {
        given()
                .when()
                .get("/reactive/multiple-combine")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto and Hello Goku"));

        SpanDataList spans = spanExporter().awaitCount(7, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        // First span is the call getting into the server. It does not have a parent span.
        SpanData parent = spans.ofKind(SERVER).roots().single();

        // We should get 2 client spans originated by the server
        SpanDataList clientSpans = spans.ofKind(CLIENT).childrenOf(parent);
        assertThat(clientSpans).hasSize(2);

        // Each client calls the server and programmatically create a span, so each have a server and an internal span

        // Naruto Span
        SpanData naruto = clientSpans.filter(span -> span.getAttributes().get(URL_FULL).contains("Naruto")).single();

        SpanData narutoServer = spans.ofKind(SERVER).childrenOf(naruto).single();
        assertThat(narutoServer)
                .hasAttribute(URL_PATH, "/reactive")
                .hasAttribute(URL_QUERY, "name=Naruto");
        SpanData narutoInternal = spans.ofKind(INTERNAL).childrenOf(narutoServer).single();
        assertThat(narutoInternal).hasName("helloGet");

        // Goku Span
        SpanData goku = clientSpans.filter(span -> span.getAttributes().get(URL_FULL).contains("Goku")).single();

        SpanData gokuServer = spans.ofKind(SERVER).childrenOf(goku).single();
        assertThat(gokuServer)
                .hasAttribute(URL_PATH, "/reactive")
                .hasAttribute(URL_QUERY, "name=Goku");
        SpanData gokuInternal = spans.ofKind(INTERNAL).childrenOf(gokuServer).single();
        assertThat(gokuInternal).hasName("helloGet");
    }

    @Test
    void multipleUsingCombineDifferentPaths() {
        given()
                .when()
                .get("/reactive/multiple-combine-different-paths")
                .then()
                .statusCode(200)
                .body(equalTo("helloGetUniDelay and Hello Naruto and Hello Goku and helloGetUniExecutor"));

        SpanDataList spans = spanExporter().awaitCount(13, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        // First span is the call getting into the server. It does not have a parent span.
        SpanData parent = spans.ofKind(SERVER).roots().single();

        // We should get 2 client spans originated by the server
        SpanDataList clientSpans = spans.ofKind(CLIENT).childrenOf(parent);
        assertThat(clientSpans).hasSize(4);

        // Each client calls the server and programmatically create a span, so each have a server and an internal span

        // helloGetUniDelay Span
        SpanData helloGetUniDelay = clientSpans
                .filter(span -> span.getAttributes().get(URL_FULL).contains("/hello-get-uni-delay"))
                .single();
        assertThat(helloGetUniDelay).hasName("GET /reactive/hello-get-uni-delay");

        SpanData helloGetUniDelayServer = spans.ofKind(SERVER).childrenOf(helloGetUniDelay).single();
        assertThat(helloGetUniDelayServer).hasAttribute(URL_PATH, "/reactive/hello-get-uni-delay");
        SpanData helloGetUniDelayInternal = spans.ofKind(INTERNAL).childrenOf(helloGetUniDelayServer).single();
        assertThat(helloGetUniDelayInternal).hasName("helloGetUniDelay");

        // Naruto Span
        SpanData naruto = clientSpans.filter(span -> span.getAttributes().get(URL_FULL).contains("Naruto")).single();
        assertThat(naruto).hasName("GET /reactive");

        SpanData narutoServer = spans.ofKind(SERVER).childrenOf(naruto).single();
        assertThat(narutoServer)
                .hasAttribute(URL_PATH, "/reactive")
                .hasAttribute(URL_QUERY, "name=Naruto");
        SpanData narutoInternal = spans.ofKind(INTERNAL).childrenOf(narutoServer).single();
        assertThat(narutoInternal).hasName("helloGet");

        // Goku Span
        SpanData goku = clientSpans.filter(span -> span.getAttributes().get(URL_FULL).contains("Goku")).single();
        assertThat(goku).hasName("GET /reactive");

        SpanData gokuServer = spans.ofKind(SERVER).childrenOf(goku).single();
        assertThat(gokuServer)
                .hasAttribute(URL_PATH, "/reactive")
                .hasAttribute(URL_QUERY, "name=Goku");
        SpanData gokuInternal = spans.ofKind(INTERNAL).childrenOf(gokuServer).single();
        assertThat(gokuInternal).hasName("helloGet");

        // helloGetUniDelay Span
        SpanData helloGetUniExecutor = clientSpans
                .filter(span -> span.getAttributes().get(URL_FULL).contains("/hello-get-uni-executor"))
                .single();
        assertThat(helloGetUniExecutor).hasName("GET /reactive/hello-get-uni-executor");

        SpanData helloGetUniExecutorServer = spans.ofKind(SERVER).childrenOf(helloGetUniExecutor).single();
        assertThat(helloGetUniExecutorServer).hasAttribute(URL_PATH, "/reactive/hello-get-uni-executor");
        SpanData helloGetUniExecutorInternal = spans.ofKind(INTERNAL).childrenOf(helloGetUniExecutorServer).single();
        assertThat(helloGetUniExecutorInternal).hasName("helloGetUniExecutor");
    }

    @Test
    public void securedInvalidCredential() {
        given().auth().preemptive().basic("scott", "reader2").when().get("/foo/secured/item/something")
                .then()
                .statusCode(401);

        SpanData span = spanExporter().awaitCount(1, Duration.ofSeconds(10)).single();
        assertThat(span).hasName("GET /{dummy}/secured/item/{value}");
        assertEvent(span, SecurityEventUtil.AUTHN_FAILURE_EVENT_NAME);
    }

    @Test
    public void securedProperCredentials() {
        given().auth().preemptive().basic("scott", "reader").when().get("/foo/secured/item/something")
                .then()
                .statusCode(200);

        SpanData span = spanExporter().awaitCount(1, Duration.ofSeconds(10)).single();
        assertThat(span).hasName("GET /{dummy}/secured/item/{value}");
        assertEvent(span, SecurityEventUtil.AUTHN_SUCCESS_EVENT_NAME, SecurityEventUtil.AUTHZ_SUCCESS_EVENT_NAME);
    }

    private static void assertEvent(SpanData span, String... expectedEventNames) {
        assertThat(span.getEvents()).extracting(EventData::getName).containsExactlyInAnyOrder(expectedEventNames);
        for (EventData event : span.getEvents()) {
            assertEventAttributes(event);
        }
    }

    private static void assertEventAttributes(EventData event) {
        switch (event.getName()) {
            case SecurityEventUtil.AUTHN_FAILURE_EVENT_NAME:
                assertEquals(AuthenticationFailedException.class.getName(),
                        event.getAttributes().get(stringKey(SecurityEventUtil.FAILURE_NAME)));
                break;
            case SecurityEventUtil.AUTHN_SUCCESS_EVENT_NAME:
            case SecurityEventUtil.AUTHZ_SUCCESS_EVENT_NAME:
                assertEquals("scott", event.getAttributes().get(stringKey(SecurityEventUtil.SECURITY_IDENTITY_PRINCIPAL)));
                assertEquals(Boolean.FALSE,
                        event.getAttributes().get(booleanKey(SecurityEventUtil.SECURITY_IDENTITY_IS_ANONYMOUS)));
                break;
            default:
                fail("Unknown event name " + event.getName());
        }
    }
}
