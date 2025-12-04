package io.quarkus.it.jaxb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AwtJaxbTest {
    static final String BOOK_WITH_IMAGE = """
            {
                "title": "Foundation",
                "cover": "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAIElEQVR4XmNgGCngPxSgi6MAZAU4FeOUQAdEKwQBdKsBOgof4SXid6kAAAAASUVORK5CYII="
            }
            """;

    /**
     * Smoke tests that we have .so files
     * copied over from the remote build container.
     */
    @Test
    void book() {
        given()
                .when()
                .contentType("application/json")
                .body(BOOK_WITH_IMAGE)
                .when()
                .post("/book")
                .then()
                .statusCode(200)
                // The height in pixels of the book's cover image.
                .body(equalTo("\"10\""));
    }

    /**
     * Smoke tests that our Lambda function makes at
     * least some sense, but it doesn't talk to real AWS API.
     */
    @Test
    void lambda() {
        given().baseUri("http://localhost:8082")
                .contentType("application/json")
                .body(BOOK_WITH_IMAGE)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(equalTo("\"10\""));
    }
}
