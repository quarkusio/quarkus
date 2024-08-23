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

    @Test
    public void testIndexInDirectory() {
        when().get("/dummy/").then().statusCode(200);
    }

    @Test
    public void testIndexInNestedDirectory() {
        when().get("/l1/l2/").then().statusCode(200);
    }

    @Test
    public void testNonIndexInDirectory() {
        when().get("/dummy2/").then().statusCode(404);
    }

    @Test
    public void testIndexInNonExistingDirectory() {
        when().get("/dummy3/").then().statusCode(404);
    }
}
