package io.quarkus.it.jaxp;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JaxpTest {

    @Test
    public void domBuilder() {
        RestAssured.given()
                .contentType("text/xml")
                .body("<foo>bar</foo>")
                .post("/jaxp/dom-builder")
                .then()
                .statusCode(200)
                .body(is("bar"));

    }

    @Test
    public void transformer() {
        RestAssured.given()
                .contentType("text/xml")
                .body("<foo>bar</foo>")
                .post("/jaxp/transformer")
                .then()
                .statusCode(200)
                .body(is("bar"));

    }

    @Test
    public void xPath() {
        RestAssured.given()
                .contentType("text/xml")
                .param("input", "<foo><bar>baz</bar></foo>")
                .param("xpath", "/foo/bar/text()")
                .get("/jaxp/xpath")
                .then()
                .statusCode(200)
                .body(is("baz"));

    }
}
