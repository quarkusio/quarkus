package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonS3Test {

    @Test
    public void testS3Async() {
        RestAssured.when().get("/test/s3/async").then().body(is("INTERCEPTED+sample S3 object"));
    }

    @Test
    public void testS3Blocking() {
        RestAssured.when().get("/test/s3/blocking").then().body(is("INTERCEPTED+sample S3 object"));
    }

}
