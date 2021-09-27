package io.quarkus.it.resteasy.elytron;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AuthAndEventLoopTest {

    @Test
    void testGet() {
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    void testGetAsync() {
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .when()
                .get("/async/uni")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void testGetAsyncWithoutAuth() {
        given()
                .when()
                .get("/async/uni")
                .then()
                .statusCode(401);
    }
}
