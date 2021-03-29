package org.acme.reactive.routes;

import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class RouteTest {

    @Test
    public void testDeclarativeRoutes() {
        RestAssured.get("/").then()
                .header("X-Header", "intercepting the request")
                .statusCode(200)
                .body(is("hello"));

        RestAssured.get("/hello").then()
                .header("X-Header", "intercepting the request")
                .statusCode(200)
                .body(is("hello world"));

        RestAssured.get("/hello?name=quarkus").then()
                .header("X-Header", "intercepting the request")
                .statusCode(200)
                .body(is("hello quarkus"));
    }

}
