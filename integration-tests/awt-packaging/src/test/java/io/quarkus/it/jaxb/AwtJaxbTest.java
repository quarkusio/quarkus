package io.quarkus.it.jaxb;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AwtJaxbTest {

    public static final String BOOK_WITH_IMAGE = "<book>" +
            "<cover>iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAIElEQVR4XmNgGCngPxSgi6MAZAU4FeOUQAdEKwQBdKsBOgof4SXid6kAAAAASUVORK5CYII=</cover>"
            +
            "<title>Foundation</title>" +
            "</book>";

    /**
     * Smoke tests that we have .so files
     * copied over from the remote build container.
     */
    @Test
    public void book() {
        given()
                .when()
                .header("Content-Type", APPLICATION_XML)
                .body(BOOK_WITH_IMAGE)
                .when()
                .post("/jaxb/book")
                .then()
                .statusCode(HttpStatus.SC_ACCEPTED)
                // The height in pixels of the book's cover image.
                .body(is("10"));
    }

    /**
     * Smoke tests that our Lambda function makes at
     * least some sense, but it doesn't talk to real AWS API.
     */
    @Test
    public void testLambdaStream() {
        assertEquals("10", LambdaClient.invoke(String.class, BOOK_WITH_IMAGE));
    }

}
