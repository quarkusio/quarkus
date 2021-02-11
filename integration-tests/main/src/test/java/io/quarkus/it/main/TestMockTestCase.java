package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class TestMockTestCase {

    @Test
    public void testMockService() {
        RestAssured.when().get("/test/service").then().body(is("mock"));
    }

}
