package io.quarkus.jfr.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;

@QuarkusTest
public class RequestIdTest {

    @Test
    public void testRequestWithoutRequestId() {
        given()
                .when().get("/requestId")
                .then()
                .statusCode(200)
                .body("id", nullValue());
    }

    @Test
    public void testRequestWithRequestId() {
        given()
                .when().header(new Header("X-Request-ID", "abc"))
                .get("/requestId")
                .then()
                .statusCode(200)
                .body("id", is("abc"));
    }
}
