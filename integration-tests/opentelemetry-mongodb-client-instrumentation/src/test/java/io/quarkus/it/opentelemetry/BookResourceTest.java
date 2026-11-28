package io.quarkus.it.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.DbAttributes.DB_QUERY_TEXT;
import static io.quarkus.it.opentelemetry.MongoSpanAttributes.optionalString;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = MongoTestResource.VERSION, value = "V6_0"))
class BookResourceTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    private final TypeRef<List<Book>> bookListType = new TypeRef<>() {
    };

    @BeforeEach
    @AfterEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    void blockingClient() {
        testInsertBooks("/books");
        reset();
        assertThat(get("/books").as(bookListType)).hasSize(3);
        assertTraceAvailable(2, "my-collection");
        assertParentChild(2, "my-collection");
    }

    @Test
    void blockingClientError() {
        given()
                .get("/books/invalid")
                .then()
                .assertThat()
                .statusCode(500);
        assertTraceAvailable(2, "my-collection", "$invalidop");
        assertParentChild(2, "my-collection");
    }

    @Test
    void reactiveClient() {
        testInsertBooks("/reactive-books");
        reset();
        assertThat(get("/reactive-books").as(bookListType)).hasSize(3);
        assertTraceAvailable(2, "my-reactive-collection");
        assertParentChild(2, "my-reactive-collection");
    }

    @Test
    void reactiveClientMultipleChain() {
        testInsertBooks("/reactive-books");
        reset();
        assertThat(get("/reactive-books/multiple-chain").as(Long.class)).isEqualTo(3L);
        assertTraceAvailable(3, "my-reactive-collection");
        assertParentChild(3, "my-reactive-collection");
    }

    @Test
    void reactiveClientMultipleCombine() {
        testInsertBooks("/reactive-books");
        reset();
        assertThat(get("/reactive-books/multiple-combine").as(Long.class)).isEqualTo(3L);
        assertTraceAvailable(3, "my-reactive-collection");
        assertParentChild(3, "my-reactive-collection");
    }

    @Test
    void reactiveClientParentChild() {
        testInsertBooks("/reactive-books");
        reset();
        assertThat(get("/reactive-books").as(bookListType)).hasSize(3);
        assertTraceAvailable(2, "my-reactive-collection");
        assertParentChild(2, "my-reactive-collection");
    }

    @Test
    void reactiveClientError() {
        given()
                .get("/reactive-books/invalid")
                .then()
                .assertThat()
                .statusCode(500);
        assertTraceAvailable(2, "my-reactive-collection", "$invalidop");
        assertParentChild(2, "my-reactive-collection");
    }

    private void assertTraceAvailable(int expectedNumOfSpans, String... commandPart) {
        SpanDataList spans = spanExporter().awaitCount(expectedNumOfSpans, Duration.ofSeconds(10));
        boolean found = false;
        for (SpanData span : spans) {
            String cmd = optionalString(span.getAttributes(), DB_QUERY_TEXT);
            if (cmd != null) {
                assertThat(cmd).contains(commandPart).contains("books");
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    private void assertParentChild(int expectedNumOfSpans, String... commandPart) {
        SpanDataList spans = spanExporter().awaitCount(expectedNumOfSpans, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).as("All spans must have the same trace id").hasSize(1);
        assertThat(spans).allSatisfy(span -> assertThat(span.getTraceId()).isNotBlank());

        SpanData rootSpan = getRootSpan(spans);
        assertThat(rootSpan.getSpanId()).isNotBlank();

        for (SpanData span : spans) {
            String cmd = optionalString(span.getAttributes(), DB_QUERY_TEXT);
            if (cmd != null) {
                assertThat(cmd).contains(commandPart).contains("books");
                assertThat(span).hasParent(rootSpan);
                assertThat(span.getSpanId()).isNotBlank().isNotEqualTo(rootSpan.getSpanId());
            }
        }

    }

    private record ChildSpanData(String traceId, String parentSpanId, String spanId) {
    }

    /**
     * find root span id
     * this is the case if the span has no valid parent
     *
     * @param spans
     * @return
     */
    private static SpanData getRootSpan(SpanDataList spans) {
        for (SpanData span : spans) {
            if (!span.getParentSpanContext().isValid()) {
                return span;
            }
        }
        throw new IllegalStateException("No root span found");
    }

    private void testInsertBooks(String endpoint) {
        given()
                .delete(endpoint)
                .then()
                .assertThat()
                .statusCode(200);

        await().atMost(Duration.ofSeconds(60L))
                .untilAsserted(() -> assertThat(get(endpoint).as(bookListType)).as("must delete all").isEmpty());

        saveBook(new Book("Victor Hugo", "Les Misérables"), endpoint);
        saveBook(new Book("Victor Hugo", "Notre-Dame de Paris"), endpoint);
        await().atMost(Duration.ofSeconds(60L))
                .untilAsserted(() -> assertThat(get(endpoint).as(bookListType)).hasSize(2));
        saveBook(new Book("Charles Baudelaire", "Les fleurs du mal"), endpoint);
        await().atMost(Duration.ofSeconds(60L))
                .untilAsserted(() -> assertThat(get(endpoint).as(bookListType)).hasSize(3));
    }

    private static void saveBook(Book book, String endpoint) {
        given()
                .header("Content-Type", "application/json")
                .body(book)
                .post(endpoint)
                .then().assertThat().statusCode(202);
    }
}
