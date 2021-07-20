package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(LibraryTestResource.class)
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