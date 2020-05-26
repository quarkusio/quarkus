package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonKmsTest {

    @Test
    public void testKmsAsync() {
        RestAssured.when().get("/test/kms/async").then().body(is("Quarkus is awsome"));
    }

    @Test
    public void testKmsSync() {
        RestAssured.when().get("/test/kms/sync").then().body(is("Quarkus is awsome"));
    }
}
