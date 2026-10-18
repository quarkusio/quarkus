package io.quarkus.it.vertx;

import static io.restassured.RestAssured.when;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class StaticResourcesTest {

    @Test
    public void testExisting() {
        when().get("/test.txt").then().statusCode(200);
    }

    @Test
    public void testNonExisting() {
        when().get("/test2.txt").then().statusCode(404);
    }
}
