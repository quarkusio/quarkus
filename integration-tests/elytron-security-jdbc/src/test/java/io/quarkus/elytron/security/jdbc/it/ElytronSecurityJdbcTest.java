package io.quarkus.elytron.security.jdbc.it;

import static org.hamcrest.Matchers.containsString;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class ElytronSecurityJdbcTest {

    public static final String CREDENTIALS = Base64.getEncoder().encodeToString("user:user".getBytes());

    @Test
    void anonymous() {
        RestAssured.given()
                .when()
                .get("/api/anonymous")
                .then()
                .statusCode(200)
                .body(containsString("anonymous"));
    }

    @Test
    void authenticated() {
        RestAssured.given()
                .when()
                .header("Authorization", "Basic " + CREDENTIALS)
                .get("/api/authenticated")
                .then()
                .statusCode(200)
                .body(containsString("authenticated"));
    }

    @Test
    void authenticated_not_authenticated() {
        RestAssured.given()
                .when()
                .get("/api/authenticated")
                .then()
                .statusCode(401);
    }

    @Test
    void forbidden() {
        RestAssured.given()
                .when()
                .header("Authorization", "Basic " + CREDENTIALS)
                .get("/api/forbidden")
                .then()
                .statusCode(403);
    }

    @Test
    void forbidden_not_authenticated() {
        RestAssured.given()
                .when()
                .get("/api/forbidden")
                .then()
                .statusCode(401);
    }

}
