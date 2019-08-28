package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ContextPropagationTestCase {

    @Test
    public void testContextPropagation() throws Exception {
        RestAssured.when().get("/context-propagation").then().body(is("OK"));
    }
}
