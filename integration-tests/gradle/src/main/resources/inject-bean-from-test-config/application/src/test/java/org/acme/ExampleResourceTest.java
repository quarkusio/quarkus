package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(value = LibraryTestResource.class, restrictToAnnotatedClass = false)
public class ExampleResourceTest {

    @Inject
    LibraryTestBean libraryBean;

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("hello"));

        assertEquals("test", libraryBean.getValue());
    }
}
