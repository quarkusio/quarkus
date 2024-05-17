package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
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
        assertTraceAvailable("my-collection");
        assertParentChild("my-collection");
    }

    @Test
    void blockingClientError() {
        given()
                .get("/books/invalid")
                .then()
                .assertThat()
                .statusCode(500);
        assertTraceAvailable("my-collection", "$invalidop");
        assertParentChild("my-collection");
    }

    @Test
    void reactiveClient() {
        testInsertBooks("/reactive-books");
        reset();
        assertThat(get("/reactive-books").as(bookListType)).hasSize(3);
        assertTraceAvailable("my-reactive-collection");
        assertParentChild("my-reactive-collection");
    }

    @Test
    void reactiveClientParentChild() {
        testInsertBooks("/reactive-books");
        reset();
        assertThat(get("/reactive-books").as(bookListType)).hasSize(3);
        assertTraceAvailable("my-reactive-collection");
        assertParentChild("my-reactive-collection");
    }

    @Test
    void reactiveClientError() {
        given()
                .get("/reactive-books/invalid")
                .then()
                .assertThat()
                .statusCode(500);
        assertTraceAvailable("my-reactive-collection", "$invalidop");
        assertParentChild("my-reactive-collection");
    }

    @SuppressWarnings("unchecked")
    private void assertTraceAvailable(String... commandPart) {
        await().atMost(Duration.ofSeconds(15L)).untilAsserted(() -> {
            List<Map<String, Object>> spans = getSpans();
            Collection<ChildSpanData> mongoSpans = new ArrayList<>();
            for (Map<String, Object> spanData : spans) {
                if (spanData.get("attributes") instanceof Map attr) {
                    var cmd = (String) attr.get("mongodb.command");
                    if (cmd != null) {
                        assertThat(cmd).contains(commandPart).contains("books");
                        var parentSpanContext = (Map<String, Object>) spanData.get("parentSpanContext");
                        mongoSpans.add(new ChildSpanData(
                                (String) spanData.get("traceId"),
                                (String) parentSpanContext.get("spanId"),
                                (String) spanData.get("spanId")));
                    }
                }
            }
            assertThat(mongoSpans).as("Mongodb statement was not traced.").isNotEmpty();
        });
    }

    @SuppressWarnings("unchecked")
    private void assertParentChild(String... commandPart) {
        await().atMost(Duration.ofSeconds(30L)).untilAsserted(() -> {
            List<Map<String, Object>> spans = getSpans();
            var traceIds = spans.stream().map(data -> data.get("traceId")).toList();
            String traceId = (String) traceIds.get(0);
            assertThat(traceId).isNotBlank();
            assertThat(traceIds).as("All spans must have the same trace id").containsOnly(traceId);

            var rootSpanId = getRootSpan(spans);
            assertThat(rootSpanId).isNotBlank();

            Collection<ChildSpanData> mongoSpans = new ArrayList<>();
            for (Map<String, Object> spanData : spans) {
                assertThat(spanData).as("span must have trace id").containsEntry("traceId", traceId);
                if (spanData.get("attributes") instanceof Map attr) {
                    var cmd = (String) attr.get("mongodb.command");
                    if (cmd != null) {
                        assertThat(cmd).contains(commandPart).contains("books");
                        var parentSpanContext = (Map<String, Object>) spanData.get("parentSpanContext");
                        mongoSpans.add(new ChildSpanData(
                                (String) spanData.get("traceId"),
                                (String) parentSpanContext.get("spanId"),
                                (String) spanData.get("spanId")));
                    }
                }
            }
            for (ChildSpanData childSpanData : mongoSpans) {
                assertThat(childSpanData.traceId()).isNotBlank().isEqualTo(traceId);
                assertThat(childSpanData.parentSpanId()).isNotBlank().isEqualTo(rootSpanId);
                assertThat(childSpanData.spanId()).isNotBlank().isNotEqualTo(rootSpanId);
            }
            assertThat(mongoSpans).as("Mongodb statement was not traced.").isNotEmpty();
        });
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
