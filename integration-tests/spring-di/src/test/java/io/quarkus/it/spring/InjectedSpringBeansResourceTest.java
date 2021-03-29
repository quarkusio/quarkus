package io.quarkus.it.spring;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
public class InjectedSpringBeansResourceTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/spring-test").then()
                .body(containsString("YOLO WORLD!"));
    }

    @Test
    public void testRequestScope() {
        RestAssured.when().get("/spring-test/request").then()
                .body(Matchers.is("0"));
        RestAssured.when().get("/spring-test/request").then()
                .body(Matchers.is("1"));
    }

    @Test
    public void testSessionScope() {
        final Response first = when().get("/spring-test/session");
        final String sessionId = first.sessionId();
        first.then()
                .statusCode(200)
                .body(Matchers.is("0"));
        RestAssured.given()
                .sessionId(sessionId)
                .when()
                .get("/spring-test/session")
                .then()
                .statusCode(200)
                .body(Matchers.is("1"));

        RestAssured.given()
                .sessionId(sessionId)
                .when()
                .post("/spring-test/invalidate")
                .then();

        final Response second = RestAssured.given()
                .sessionId(sessionId)
                .when()
                .get("/spring-test/session");
        assertNotEquals(sessionId, second.sessionId());
        second.then()
                .statusCode(200)
                .body(Matchers.is("0"));
    }

    @Test
    public void testPrototypeScope() {
        RestAssured.when().get("/spring-test/prototype").then()
                .body(Matchers.containsString("0"))
                .body(Matchers.containsString("1"));
    }
}
