package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FilterTestCase {

    @Test
    void testAnyPathAdditionalHeadersGet() {
        given()
                .get("/filter/any")
                .then()
                .statusCode(200)
                .header("Cache-Control", is("max-age=31536000"))
                .body(is("ok"));

    }

    @Test
    void testAnyPathAdditionalHeadersHead() {
        // HEAD requests should not include the header
        given()
                .head("/filter/any")
                .then()
                .statusCode(200)
                .header("Cache-Control", is(emptyOrNullString()));

    }

    @Test
    void testAnotherPathAdditionalHeadersGet() {
        given()
                .get("/filter/another")
                .then()
                .statusCode(200)
                .header("Cache-Control", is("max-age=31536000"))
                .body(is("ok"));

    }

    void testAnotherPathAdditionalHeadersHead() {
        given()
                .head("/filter/another")
                .then()
                .statusCode(200)
                .header("Cache-Control", is(emptyOrNullString()));

    }

    @Test
    void testAdditionalHeadersOverride() {
        given()
                .get("/filter/override")
                .then()
                .statusCode(200)
                .header("Cache-Control", is("max-age=0"))
                .body(is("ok"));
    }

    @Test
    void testCacheControlIsSent() {
        given()
                .get("/filter/no-cache")
                .then()
                .statusCode(200)
                .header("Cache-Control", is("none"))
                .body(is("ok"));

    }

    @Test
    void testPathOrder() {
        given()
                .get("/filter/order")
                .then()
                .statusCode(200)
                .header("Cache-Control", is("max-age=1"))
                .body(is("ok"));

    }

}
