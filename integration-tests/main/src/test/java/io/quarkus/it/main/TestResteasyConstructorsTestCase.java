package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class TestResteasyConstructorsTestCase {

    @Test
    public void testWithoutDefaultConstructor() {
        RestAssured.when().get("/testCtor/service").then().body(is("some"));
    }

    @Test
    public void testWithDefaultConstructor() {
        RestAssured.when().get("/testCtor2/service").then().body(is("some"));
    }
}
