package io.quarkus.spring.web.requestheader;

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
}
