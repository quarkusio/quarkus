package io.quarkus.jfr.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
public class JfrTest {

    private static final String CLIENT = "127.0.0.1:\\d{1,5}";
    private static final String RESOURCE_CLASS = "io.quarkus.jfr.it.AppResource";

    @Test
    public void blockingTest() {
        String jfrName = "blockingTest";

        final String url = "/app/blocking";

        given()
                .when().get("/jfr/start/" + jfrName)
                .then()
                .statusCode(204);

        IdResponse response = given()
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .extract()
                .as(IdResponse.class);

        given()
                .when().get("/jfr/stop/" + jfrName)
                .then()
                .statusCode(204);

        final String resourceMethod = "blocking";

        ValidatableResponse validatableResponse = given()
                .when().get("/jfr/check/" + jfrName + "/traceId/" + response.traceId)
                .then()
                .statusCode(200)
                .body("start", notNullValue())
                .body("start.uri", is(url))
                .body("start.traceId", is(response.traceId))
                .body("start.spanId", is(response.spanId))
                .body("start.httpMethod", is("GET"))
                .body("start.resourceClass", is(RESOURCE_CLASS))
                .body("start.resourceMethod", is(resourceMethod))
                .body("start.client", matchesRegex(CLIENT))
                .body("end", notNullValue())
                .body("end.uri", is(url))
                .body("end.traceId", is(response.traceId))
                .body("end.spanId", is(response.spanId))
                .body("end.httpMethod", is("GET"))
                .body("end.resourceClass", is(RESOURCE_CLASS))
                .body("end.resourceMethod", is(resourceMethod))
                .body("end.client", matchesRegex(CLIENT))
                .body("period", notNullValue())
                .body("period.uri", is(url))
                .body("period.traceId", is(response.traceId))
                .body("period.spanId", is(response.spanId))
                .body("period.httpMethod", is("GET"))
                .body("period.resourceClass", is(RESOURCE_CLASS))
                .body("period.resourceMethod", is(resourceMethod))
                .body("period.client", matchesRegex(CLIENT));
    }

    @Test
    public void reactiveTest() {
        String jfrName = "reactiveTest";

        final String url = "/app/reactive";

        given()
                .when().get("/jfr/start/" + jfrName)
                .then()
                .statusCode(204);

        IdResponse response = given()
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .extract().as(IdResponse.class);

        given()
                .when().get("/jfr/stop/" + jfrName)
                .then()
                .statusCode(204);

        final String resourceMethod = "reactive";

        ValidatableResponse validatableResponse = given()
                .when().get("/jfr/check/" + jfrName + "/traceId/" + response.traceId)
                .then()
                .statusCode(200)
                .body("start", notNullValue())
                .body("start.uri", is(url))
                .body("start.traceId", is(response.traceId))
                .body("start.spanId", is(response.spanId))
                .body("start.httpMethod", is("GET"))
                .body("start.resourceClass", is(RESOURCE_CLASS))
                .body("start.resourceMethod", is(resourceMethod))
                .body("start.client", matchesRegex(CLIENT))
                .body("end", notNullValue())
                .body("end.uri", is(url))
                .body("end.traceId", is(response.traceId))
                .body("end.spanId", is(response.spanId))
                .body("end.httpMethod", is("GET"))
                .body("end.resourceClass", is(RESOURCE_CLASS))
                .body("end.resourceMethod", is(resourceMethod))
                .body("end.client", matchesRegex(CLIENT))
                .body("period", notNullValue())
                .body("period.uri", is(url))
                .body("period.traceId", is(response.traceId))
                .body("period.spanId", is(response.spanId))
                .body("period.httpMethod", is("GET"))
                .body("period.resourceClass", is(RESOURCE_CLASS))
                .body("period.resourceMethod", is(resourceMethod))
                .body("period.client", matchesRegex(CLIENT));
    }

    @Test
    public void errorTest() {
        String jfrName = "errorTest";

        final String url = "/app/error";

        given()
                .when().get("/jfr/start/" + jfrName)
                .then()
                .statusCode(204);

        String traceId = given()
                .when()
                .get(url)
                .then()
                .statusCode(500)
                .extract().asString();

        given()
                .when().get("/jfr/stop/" + jfrName)
                .then()
                .statusCode(204);

        final String resourceMethod = "error";

        ValidatableResponse validatableResponse = given()
                .when().get("/jfr/check/" + jfrName + "/traceId/" + traceId)
                .then()
                .statusCode(200)
                .body("start", notNullValue())
                .body("start.uri", is(url))
                .body("start.traceId", is(traceId))
                .body("start.spanId", nullValue())
                .body("start.httpMethod", is("GET"))
                .body("start.resourceClass", is(RESOURCE_CLASS))
                .body("start.resourceMethod", is(resourceMethod))
                .body("start.client", matchesRegex(CLIENT))
                .body("end", notNullValue())
                .body("end.uri", is(url))
                .body("end.traceId", is(traceId))
                .body("end.spanId", is(nullValue()))
                .body("end.httpMethod", is("GET"))
                .body("end.resourceClass", is(RESOURCE_CLASS))
                .body("end.resourceMethod", is(resourceMethod))
                .body("end.client", matchesRegex(CLIENT))
                .body("period", notNullValue())
                .body("period.uri", is(url))
                .body("period.traceId", is(traceId))
                .body("period.spanId", is(nullValue()))
                .body("period.httpMethod", is("GET"))
                .body("period.resourceClass", is(RESOURCE_CLASS))
                .body("period.resourceMethod", is(resourceMethod))
                .body("period.client", matchesRegex(CLIENT));
    }

    @Test
    public void nonExistURL() {
        String jfrName = "nonExistURL";

        final String url = "/nonExistURL";

        given()
                .when().get("/jfr/start/" + jfrName)
                .then()
                .statusCode(204);

        given()
                .when()
                .get(url)
                .then()
                .statusCode(404);

        given()
                .when().get("/jfr/stop/" + jfrName)
                .then()
                .statusCode(204);

        Long count = given()
                .when().get("/jfr/count/" + jfrName).then()
                .statusCode(200)
                .extract().as(Long.class);

        Assertions.assertEquals(0, count);
    }

    @Test
    public void invalidHttpMethod() {
        String jfrName = "invalidHttpMethodTest";

        given()
                .when().get("/jfr/start/" + jfrName)
                .then()
                .statusCode(204);

        String url = "/app/blocking";
        given()
                .when()
                .post(url)
                .then()
                .statusCode(405);

        given()
                .when().get("/jfr/stop/" + jfrName)
                .then()
                .statusCode(204);

        given()
                .header("uri", url)
                .when().get("/jfr/check/" + jfrName + "/uri")
                .then()
                .statusCode(200)
                .body("start", notNullValue())
                .body("start.uri", is(url))
                .body("start.traceId", notNullValue())
                .body("start.spanId", nullValue())
                .body("start.httpMethod", is("POST"))
                .body("start.resourceClass", nullValue())
                .body("start.resourceMethod", nullValue())
                .body("start.client", matchesRegex(CLIENT))
                .body("end", notNullValue())
                .body("end.uri", is(url))
                .body("end.traceId", notNullValue())
                .body("end.spanId", nullValue())
                .body("end.httpMethod", is("POST"))
                .body("end.resourceClass", nullValue())
                .body("end.resourceMethod", nullValue())
                .body("end.client", matchesRegex(CLIENT))
                .body("period", notNullValue())
                .body("period.uri", is(url))
                .body("period.traceId", notNullValue())
                .body("period.spanId", nullValue())
                .body("period.httpMethod", is("POST"))
                .body("period.resourceClass", nullValue())
                .body("period.resourceMethod", nullValue())
                .body("period.client", matchesRegex(CLIENT));
    }

    @Test
    public void unhandledContentType() {
        String jfrName = "unhandledContentType";

        given()
                .when().get("/jfr/start/" + jfrName)
                .then()
                .statusCode(204);

        String url = "/app/consuming";
        given()
                .contentType("text/plain")
                .body("whatever")
                .when()
                .post(url)
                .then()
                .statusCode(415);

        given()
                .when().get("/jfr/stop/" + jfrName)
                .then()
                .statusCode(204);

        given()
                .header("uri", url)
                .when().get("/jfr/check/" + jfrName + "/uri")
                .then()
                .statusCode(200)
                .body("start", notNullValue())
                .body("start.uri", is(url))
                .body("start.traceId", notNullValue())
                .body("start.spanId", nullValue())
                .body("start.httpMethod", is("POST"))
                .body("start.resourceClass", nullValue())
                .body("start.resourceMethod", nullValue())
                .body("start.client", matchesRegex(CLIENT))
                .body("end", notNullValue())
                .body("end.uri", is(url))
                .body("end.traceId", notNullValue())
                .body("end.spanId", nullValue())
                .body("end.httpMethod", is("POST"))
                .body("end.resourceClass", nullValue())
                .body("end.resourceMethod", nullValue())
                .body("end.client", matchesRegex(CLIENT))
                .body("period", notNullValue())
                .body("period.uri", is(url))
                .body("period.traceId", notNullValue())
                .body("period.spanId", nullValue())
                .body("period.httpMethod", is("POST"))
                .body("period.resourceClass", nullValue())
                .body("period.resourceMethod", nullValue())
                .body("period.client", matchesRegex(CLIENT));
    }

}
