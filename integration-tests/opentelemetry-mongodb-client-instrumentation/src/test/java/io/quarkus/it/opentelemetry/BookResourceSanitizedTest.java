package io.quarkus.it.opentelemetry;

import static io.opentelemetry.semconv.DbAttributes.DB_COLLECTION_NAME;
import static io.opentelemetry.semconv.DbAttributes.DB_QUERY_TEXT;
import static io.quarkus.it.opentelemetry.MongoSpanAttributes.optionalString;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
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
@TestProfile(BookResourceSanitizedTest.SanitizedProfile.class)
@QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = MongoTestResource.VERSION, value = "V6_0"))
class BookResourceSanitizedTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    public static class SanitizedProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "sanitized";
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
    void blockingClientSanitized() {
        testInsertBooks("/books");
        reset();
        assertThat(get("/books").as(bookListType)).hasSize(3);
        assertSanitizedCommandPresent(2);
    }

    @Test
    void reactiveClientSanitized() {
        testInsertBooks("/reactive-books");
        reset();
        assertThat(get("/reactive-books").as(bookListType)).hasSize(3);
        assertSanitizedCommandPresent(2);
    }

    @Test
    void commandDoesNotContainSensitiveData() {
        testInsertBooks("/books");
        reset();
        assertThat(get("/books").as(bookListType)).hasSize(3);

        spanExporter().awaitUntil("at least one span", Duration.ofSeconds(10L), spans -> !spans.isEmpty());
        boolean found = false;
        for (SpanData span : spanExporter().fetch()) {
            String cmd = optionalString(span.getAttributes(), DB_QUERY_TEXT);
            if (cmd != null) {
                // Command should not contain actual book data
                assertThat(cmd)
                        .doesNotContain("Victor Hugo")
                        .doesNotContain("Les Misérables")
                        .doesNotContain("Notre-Dame de Paris")
                        .doesNotContain("Charles Baudelaire")
                        .doesNotContain("Les fleurs du mal");

                // But should contain type placeholders
                assertThat(cmd).contains("<string>");
                found = true;
            }
        }
        assertThat(found).as("At least one span should contain a sanitized db.query.text").isTrue();
    }

    @Test
    void collectionNamePresent() {
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
        assertThat(found).as("At least one span should have db.collection.name set").isTrue();
    }

    private void assertSanitizedCommandPresent(int expectedNumOfSpans) {
        SpanDataList spans = spanExporter().awaitCount(expectedNumOfSpans, Duration.ofSeconds(10));
        boolean found = false;
        for (SpanData span : spans) {
            String cmd = optionalString(span.getAttributes(), DB_QUERY_TEXT);
            if (cmd != null) {
                // Verify command structure is present but values are sanitized
                assertThat(cmd).contains("<string>");
                found = true;
            }
        }
        assertThat(found).isTrue();
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
