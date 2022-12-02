package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ExternalIndexTestCase {

    @Test
    public void testJAXRSResourceFromExternalLibrary() {
        RestAssured.when().get("/shared").then()
                .body(is("Shared Resource"));
    }

    @Test
    public void testTransformedExternalResources() {
        RestAssured.when().get("/shared/transformed").then().body(is("Transformed Endpoint"));
    }
}
