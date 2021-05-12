package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSsmTest {

    @Test
    public void testSsmAsync() {
        RestAssured.when().get("/test/ssm/async").then().body(is("Quarkus is awsome"));
    }

    @Test
    public void testSsmSync() {
        RestAssured.when().get("/test/ssm/sync").then().body(is("Quarkus is awsome"));
    }
}
