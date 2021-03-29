package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.any;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonIamTest {

    @Test
    public void testSesAsync() {
        RestAssured.when().get("/test/iam/async").then().body(any(String.class));
    }

    @Test
    public void testSesSync() {
        RestAssured.when().get("/test/iam/sync").then().body(any(String.class));
    }
}
