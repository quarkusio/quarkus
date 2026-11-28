package io.quarkus.it.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.DbAttributes.DB_COLLECTION_NAME;
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

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@TestProfile(BookResourceOffTest.OffProfile.class)
@QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = MongoTestResource.VERSION, value = "V6_0"))
class BookResourceOffTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    public static class OffProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "off";
        }
    }

    private final TypeRef<List<Book>> bookListType = new TypeRef<>() {
    };

    @BeforeEach
    @AfterEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    void blockingClientNoCommand() {
        testInsertBooks("/books");
        reset();
        assertThat(get("/books").as(bookListType)).hasSize(3);
        assertNoCommandInSpans(2);
    }

    @Test
    void reactiveClientNoCommand() {
        testInsertBooks("/reactive-books");
        reset();
        assertThat(get("/reactive-books").as(bookListType)).hasSize(3);
        assertNoCommandInSpans(2);
    }

    @Test
    void collectionNameStillPresent() {
        testInsertBooks("/books");
        reset();
        assertThat(get("/books").as(bookListType)).hasSize(3);

        spanExporter().awaitUntil("at least one span", Duration.ofSeconds(10L), spans -> !spans.isEmpty());
        boolean found = false;
        for (SpanData span : spanExporter().fetch()) {
            String collectionName = optionalString(span.getAttributes(), DB_COLLECTION_NAME);
            if (collectionName != null) {
                assertThat(collectionName).isEqualTo("my-collection");
                found = true;
            }
        }
        assertThat(found).as("Collection name should still be present in OFF mode").isTrue();
    }

    @Test
    void spanStillCreated() {
        testInsertBooks("/books");
        reset();
        assertThat(get("/books").as(bookListType)).hasSize(3);

        // Verify spans are still created even in OFF mode
        SpanDataList spans = spanExporter().awaitCount(2, Duration.ofSeconds(10));
        assertThat(spans).isNotEmpty();
    }

    private void assertNoCommandInSpans(int expectedNumOfSpans) {
        for (SpanData span : spanExporter().awaitCount(expectedNumOfSpans, Duration.ofSeconds(10))) {
            // Command attribute should not be present in OFF mode
            assertThat(span.getAttributes()).as("Command should not be present in OFF mode")
                    .doesNotContainKey(DB_QUERY_TEXT.getKey());
        }
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
