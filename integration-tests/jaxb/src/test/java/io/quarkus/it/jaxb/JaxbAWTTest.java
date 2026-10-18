package io.quarkus.it.jaxb;

import static io.quarkus.it.jaxb.JaxbAWTIT.BOOK_WITH_IMAGE;
import static io.quarkus.it.jaxb.JaxbAWTIT.CONTENT_TYPE;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JaxbAWTTest {
    @Test
    public void book() {
        RestAssured.given()
                .when()
                .header("Content-Type", CONTENT_TYPE)
                .body(BOOK_WITH_IMAGE)
                .when()
                .post("/jaxb/book")
                .then()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body(is("10"));
    }
}
