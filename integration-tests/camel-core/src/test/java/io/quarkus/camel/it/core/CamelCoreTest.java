package io.quarkus.camel.it.core;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class CamelCoreTest {
    @Test
    public void testRoutes() {
        RestAssured.when().get("/test/routes").then().body(containsString("timer"));
    }

    @Test
    public void testProperties() {
        RestAssured.when().get("/test/property/camel.context.name").then().body(is("quarkus-camel-example"));
    }
}
