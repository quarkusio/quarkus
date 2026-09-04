package io.quarkus.spring.web.resteasy.classic.test.requestheader;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class RequestHeaderControllerTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RequestHeaderController.class));

    @Test
    public void testHeaderIsReadFromHeader() {
        given()
                .header("X-Custom-Header", "hello")
                .when().get("/api/header")
                .then()
                .statusCode(200)
                .body(is("Header: hello"));
    }

    @Test
    public void testHeaderNotReadFromQueryParam() {
        given()
                .queryParam("X-Custom-Header", "hello")
                .when().get("/api/header")
                .then()
                .statusCode(200)
                .body(is("Header: null"));
    }

    @Test
    public void testHeaderWithDefaultValue() {
        given()
                .header("X-Optional-Header", "custom")
                .when().get("/api/header/default")
                .then()
                .statusCode(200)
                .body(is("Header: custom"));

        given()
                .when().get("/api/header/default")
                .then()
                .statusCode(200)
                .body(is("Header: fallback"));
    }

    @Test
    public void testNamedByNameHeader() {
        given()
                .header("X-Other-Header", "other-value")
                .when().get("/api/headers/namedByName")
                .then()
                .statusCode(200)
                .body(is("Other-Header: other-value"));
    }

    @Test
    public void testMixedParams() {
        given()
                .header("X-Token", "secret123")
                .queryParam("filter", "active")
                .when().get("/api/headers/mixed/42")
                .then()
                .statusCode(200)
                .body(is("id=42 filter=active token=secret123"));
    }

}
