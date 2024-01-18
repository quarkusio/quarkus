package io.quarkus.jfr.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
public class JfrTest {

    @Test
    public void blockingTest() {
        String jfrName = "blockingTest";

        given()
                .when().get("/jfr/start/" + jfrName)
                .then()
                .statusCode(204);

        IdResponse response = given()
                .when()
                .get("/app/blocking")
                .then()
                .statusCode(200)
                .extract()
                .as(IdResponse.class);
        System.out.println(response);

        given()
                .when().get("/jfr/stop/" + jfrName)
                .then()
                .statusCode(204);

        ValidatableResponse validatableResponse = given()
                .when().get("/jfr/check/" + jfrName + "/" + response.traceId)
                .then()
                .statusCode(200)
                .body("start", nullValue())
                .body("end", nullValue())
                .body("blocking", notNullValue())
                .body("blocking.uri", is("/app/blocking"))
                .body("blocking.traceId", is(response.traceId))
                .body("blocking.spanId", is(response.spanId))
                .body("blocking.httpMethod", is("GET"))
                .body("blocking.resourceClass", is("io.quarkus.jfr.it.AppResource"))
                .body("blocking.resourceMethod",
                        is("public io.quarkus.jfr.it.IdResponse io.quarkus.jfr.it.AppResource.blocking()"))
                .body("blocking.client", matchesRegex("127.0.0.1:\\d{1,5}"));
    }

    @Test
    public void reactiveTest() {
        String jfrName = "reactiveTest";

        given()
                .when().get("/jfr/start/" + jfrName)
                .then()
                .statusCode(204);

        IdResponse response = given()
                .when()
                .get("/app/reactive")
                .then()
                .statusCode(200)
                .extract().as(IdResponse.class);

        given()
                .when().get("/jfr/stop/" + jfrName)
                .then()
                .statusCode(204);

        ValidatableResponse validatableResponse = given()
                .when().get("/jfr/check/" + jfrName + "/" + response.traceId)
                .then()
                .statusCode(200)
                .body("start", notNullValue())
                .body("start.uri", is("/app/reactive"))
                .body("start.traceId", is(response.traceId))
                .body("start.spanId", is(response.spanId))
                .body("start.httpMethod", is("GET"))
                .body("start.resourceClass", is("io.quarkus.jfr.it.AppResource"))
                .body("start.resourceMethod",
                        is("public io.smallrye.mutiny.Uni<io.quarkus.jfr.it.IdResponse> io.quarkus.jfr.it.AppResource.reactive()"))
                .body("start.client", matchesRegex("127.0.0.1:\\d{1,5}"))
                .body("end", notNullValue())
                .body("end.traceId", is(response.traceId))
                .body("end.spanId", is(response.spanId))
                .body("end.processDuration", greaterThan(0))
                .body("blocking", nullValue());
    }
}
