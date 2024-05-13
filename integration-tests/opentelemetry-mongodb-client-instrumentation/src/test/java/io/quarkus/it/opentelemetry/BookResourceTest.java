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
        assertTraceAvailable("my-collection");
    }

    @Test
    void reactiveClient() {
        testInsertBooks("/reactive-books");
        assertTraceAvailable("my-reactive-collection");
    }

    private void assertTraceAvailable(String dbCollectionName) {
        await().atMost(Duration.ofSeconds(30L)).untilAsserted(() -> {
            boolean traceAvailable = false;
            for (Map<String, Object> spanData : getSpans()) {
                if (spanData.get("attributes") instanceof Map attr) {
                    var cmd = (String) attr.get("mongodb.command");
                    if (cmd != null) {
                        assertThat(cmd).contains(dbCollectionName, "books");
                        traceAvailable = true;
                    }
                }
            }
            assertThat(traceAvailable).as("Mongodb statement was not traced.").isTrue();
        });
    }

    private void testInsertBooks(String endpoint) {
        given()
                .delete(endpoint)
                .then()
                .assertThat()
                .statusCode(200);

        assertThat(get(endpoint).as(bookListType)).isEmpty();

        saveBook(new Book("Victor Hugo", "Les Misérables"), endpoint);
        saveBook(new Book("Victor Hugo", "Notre-Dame de Paris"), endpoint);
        await().atMost(Duration.ofSeconds(60L))
                .untilAsserted(() -> assertThat(get(endpoint).as(bookListType)).hasSize(2));

        saveBook(new Book("Charles Baudelaire", "Les fleurs du mal"), endpoint);

        assertThat(get(endpoint).as(bookListType)).hasSize(3);

        List<Book> books = get("%s/Victor Hugo".formatted(endpoint)).as(bookListType);
        assertThat(books).hasSize(2);
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
