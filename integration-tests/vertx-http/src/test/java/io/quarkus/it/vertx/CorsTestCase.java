package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CorsTestCase {
    @Test
    void basicPreflightTest() {
        given()
                .header("Origin", "https://example.org/")
                .header("Access-Control-Request-Method", "GET")
                .options("/simple/options")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Methods", is("GET"))
                .header("access-control-allow-origin", is("https://example.org/"))
                .header("access-control-allow-credentials", is("false"))
                .header("content-length", is("0"))
                .body(is(""));
    }

    @Test
    void preflightWithHeadersOnlyTest() {
        given()
                .header("Origin", "https://example.org/")
                .header("Access-Control-Request-Headers", "X-Custom-Header")
                .options("/simple/options")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Headers", is("X-Custom-Header"))
                .header("access-control-allow-origin", is("https://example.org/"))
                .header("access-control-allow-credentials", is("false"))
                .header("content-length", is("0"))
                .body(is(""));
    }

    @Test
    void optionsWithCORSHeaderButNoOrigin() {
        given()
                .header("Access-Control-Request-Method", "GET")
                .options("/simple/options")
                .then()
                .statusCode(200)
                .header("content-length", is("7"))
                .body(is("options"));
    }

    @Test
    void nonPreflightOptionsTest() {
        given()
                .header("Origin", "https://example.org/")
                .options("/simple/options")
                .then()
                .statusCode(200)
                .header("access-control-allow-origin", is("https://example.org/"))
                .header("content-length", is("7"))
                .body(is("options"));
    }
}
