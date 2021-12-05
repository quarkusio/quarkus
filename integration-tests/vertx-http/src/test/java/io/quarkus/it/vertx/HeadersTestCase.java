package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HeadersTestCase {

    @Test
    void testAdditionalHeaders() {
        given()
                .get("/headers/any")
                .then()
                .header("foo", is("bar"))
                .header("Pragma", emptyOrNullString())
                .body(is("ok"));

    }

    @Test
    void testAdditionalHeadersOverride() {
        given()
                .get("/headers/override")
                .then()
                .header("foo", is("abc"))
                .header("Pragma", emptyOrNullString())
                .body(is("ok"));
    }

    @Test
    void testPragmaIsSent() {
        given()
                .get("/headers/pragma")
                .then()
                .header("foo", is("bar"))
                .header("Pragma", is("no-cache"))
                .body(is("ok"));

    }

}
