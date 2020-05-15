package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonDynamodbTest {

    @Test
    public void testDynamoDbAsync() {
        RestAssured.when().get("/test/dynamodb/async").then().body(is("INTERCEPTED OK"));
    }

    @Test
    public void testDynamoDbBlocking() {
        RestAssured.when().get("/test/dynamodb/blocking").then().body(is("INTERCEPTED OK"));
    }
}
