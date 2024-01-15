package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;

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
    void testCorsRequest() {
        List<Header> corsMethods = given()
                .header("Origin", "https://example.org/")
                .header("Access-Control-Request-Method", "GET")
                .get("/filter/any")
                .then()
                .statusCode(200)
                .header("Cache-Control", is("max-age=31536000"))
                .header("Access-Control-Allow-Origin",
                        is("https://example.org/")) // this same header was added by cors but also by a property, we should only have one value
                .body(is("ok"))
                .extract()
                .headers().getList("Access-Control-Allow-Methods");
        Assertions.assertThat(corsMethods.stream().map(Header::getValue)).containsExactly("POST,GET,PUT,OPTIONS,DELETE",
                "TEST");

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
