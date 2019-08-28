package io.quarkus.it.main;

import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JaxbTestCase {

    @Test
    public void testNews() {
        RestAssured.when().get("/test/jaxb/getnews").then()
                .body("author", contains("Emmanuel Bernard"));
    }
}
