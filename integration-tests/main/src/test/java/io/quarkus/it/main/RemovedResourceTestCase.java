package io.quarkus.it.main;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class RemovedResourceTestCase {

    @Test
    public void testRemovedResource() {
        RestAssured.get("/removed").then().statusCode(404);
    }
}
