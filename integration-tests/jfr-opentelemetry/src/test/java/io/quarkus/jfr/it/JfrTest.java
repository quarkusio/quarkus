package io.quarkus.jfr.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
public class JfrTest {

    private static final String REQUEST_ID = "abc";

    @AfterEach
    public void reset() {
        given()
                .when().get("/jfr/reset")
                .then()
                .statusCode(204);
    }

    @Test
    public void blockingTest() {
        given()
                .when().get("/jfr/start")
                .then()
                .statusCode(204);

        given()
                .when()
                .header(new Header("X-Request-ID", REQUEST_ID))
                .get("/app/blocking")
                .then()
                .statusCode(200);

        given()
                .when().get("/jfr/stop")
                .then()
                .statusCode(204);

        ValidatableResponse validatableResponse = given()
                .when().get("/jfr/check")
                .then()
                .statusCode(200)
                .body("start", nullValue())
                .body("end", nullValue())
                .body("blocking", notNullValue())
                .body("blocking.uri", is("/app/blocking"))
                .body("blocking.requestId", is("abc"))
                .body("blocking.traceId", matchesRegex("[0-9a-f]{32}"))
                .body("blocking.spanId", matchesRegex("[0-9a-f]{16}"))
                .body("blocking.httpMethod", is("GET"))
                .body("blocking.resourceClass", is("io.quarkus.jfr.it.AppResource"))
                .body("blocking.resourceMethod", is("public java.lang.String io.quarkus.jfr.it.AppResource.blocking()"))
                .body("blocking.client", matchesRegex("127.0.0.1:\\d{1,5}"));
    }

    @Test
    public void reactiveTest() {
        given()
                .when().get("/jfr/start")
                .then()
                .statusCode(204);

        given()
                .when()
                .header(new Header("X-Request-ID", REQUEST_ID))
                .get("/app/reactive")
                .then()
                .statusCode(200);

        given()
                .when().get("/jfr/stop")
                .then()
                .statusCode(204);

        ValidatableResponse validatableResponse = given()
                .when().get("/jfr/check")
                .then()
                .statusCode(200)
                .body("start", notNullValue())
                .body("start.uri", is("/app/reactive"))
                .body("start.requestId", is("abc"))
                .body("start.traceId", matchesRegex("[0-9a-f]{32}"))
                .body("start.spanId", matchesRegex("[0-9a-f]{16}"))
                .body("start.httpMethod", is("GET"))
                .body("start.resourceClass", is("io.quarkus.jfr.it.AppResource"))
                .body("start.resourceMethod",
                        is("public io.smallrye.mutiny.Uni<java.lang.String> io.quarkus.jfr.it.AppResource.reactive()"))
                .body("start.client", matchesRegex("127.0.0.1:\\d{1,5}"))
                .body("end", notNullValue())
                .body("end.requestId", is("abc"))
                .body("end.processDuration", greaterThan(0))
                .body("blocking", nullValue());
    }
}
