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
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@TestProfile(BookResourceOffTest.OffProfile.class)
@QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = MongoTestResource.VERSION, value = "V6_0"))
class BookResourceOffTest {

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

        await().atMost(Duration.ofSeconds(10L)).until(() -> getSpans().size() >= 1);
        boolean found = false;
        for (Map<String, Object> spanData : getSpans()) {
            if (spanData.get("attributes") instanceof Map attr) {
                var collectionName = (String) attr.get("db.collection.name");
                if (collectionName != null) {
                    assertThat(collectionName).isEqualTo("my-collection");
                    found = true;
                }
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
        await().atMost(Duration.ofSeconds(10L)).until(() -> getSpans().size() == 2);
        assertThat(getSpans()).isNotEmpty();
    }

    private void assertNoCommandInSpans(int expectedNumOfSpans) {
        await().atMost(Duration.ofSeconds(10L)).until(() -> getSpans().size() == expectedNumOfSpans);
        for (Map<String, Object> spanData : getSpans()) {
            if (spanData.get("attributes") instanceof Map attr) {
                var cmd = (String) attr.get("db.query.text");
                // Command attribute should not be present in OFF mode
                assertThat(cmd).as("Command should not be present in OFF mode").isNull();
            }
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

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }
}
