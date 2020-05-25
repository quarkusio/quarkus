package org.test;

import io.quarkus.test.junit.*;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class HelloResourceTest {

    @Test
    public void testCustomizeCalled() {
        given()
                .when().get("/jpa-build-info/customizeCalled")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    public void testAllManagedClasses() {
        given()
                .when().get("/jpa-build-info/all")
                .then()
                .statusCode(200)
                .body(is("[\"org.test.SomeEmbeddable\",\"org.test.SomeEntity\"]"));
    }

    @Test
    public void testEntities() {
        given()
                .when().get("/jpa-build-info/entities")
                .then()
                .statusCode(200)
                .body(is("[\"org.test.SomeEntity\"]"));
    }

    @Test
    public void testOthers() {
        given()
                .when().get("/jpa-build-info/others")
                .then()
                .statusCode(200)
                .body(is("[\"org.test.SomeEmbeddable\"]"));
    }

}
