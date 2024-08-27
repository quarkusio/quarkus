package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = MongoTestResource.VERSION, value = "V6_0"))
class BookResourceTest {

    private final TypeRef<List<Book>> bookListType = new TypeRef<>() {
    };

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(Duration.ofSeconds(30L)).until(() -> {
            // make sure spans are cleared
            List<Map<String, Object>> spans = getSpans();
            if (!spans.isEmpty()) {
                given().get("/reset").then().statusCode(HTTP_OK);
            }
            return spans.isEmpty();
        });
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
        await().atMost(Duration.ofSeconds(10L)).until(() -> getSpans().size() == expectedNumOfSpans);
        boolean found = false;
        for (Map<String, Object> spanData : getSpans()) {
            if (spanData.get("attributes") instanceof Map attr) {
                var cmd = (String) attr.get("mongodb.command");
                if (cmd != null) {
                    assertThat(cmd).contains(commandPart).contains("books");
                    found = true;
                }
            }
        }
        assertThat(found).isTrue();
    }

    @SuppressWarnings("unchecked")
    private void assertParentChild(int expectedNumOfSpans, String... commandPart) {
        await().atMost(Duration.ofSeconds(10L)).until(() -> getSpans().size() == expectedNumOfSpans);
        List<Map<String, Object>> spans = getSpans();
        var traceIds = spans.stream().map(data -> data.get("traceId")).toList();
        String traceId = (String) traceIds.get(0);
        assertThat(traceId).isNotBlank();
        assertThat(traceIds).as("All spans must have the same trace id").containsOnly(traceId);

        var rootSpanId = getRootSpan(spans);
        assertThat(rootSpanId).isNotBlank();

        for (Map<String, Object> spanData : spans) {
            assertThat(spanData).as("span must have trace id").containsEntry("traceId", traceId);
            if (spanData.get("attributes") instanceof Map attr) {
                var cmd = (String) attr.get("mongodb.command");
                if (cmd != null) {
                    assertThat(cmd).contains(commandPart).contains("books");
                    var parentSpanContext = (Map<String, Object>) spanData.get("parentSpanContext");
                    assertThat((String) spanData.get("traceId")).isNotBlank().isEqualTo(traceId);
                    assertThat((String) parentSpanContext.get("spanId")).isNotBlank().isEqualTo(rootSpanId);
                    assertThat((String) spanData.get("spanId")).isNotBlank().isNotEqualTo(rootSpanId);
                }
            }
        }

    }

    private record ChildSpanData(String traceId, String parentSpanId, String spanId) {
    }

    /**
     * find root span id
     * this is the case if the trace id in parentSpanContext contains only zeros
     *
     * @param spans
     * @return
     */
    @SuppressWarnings("unchecked")
    private static String getRootSpan(Iterable<Map<String, Object>> spans) {
        for (Map<String, Object> spanData : spans) {
            var parentContext = (Map<String, Object>) spanData.get("parentSpanContext");
            if (((String) parentContext.get("traceId")).matches("0+")) {
                return (String) spanData.get("spanId");
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

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }
}
