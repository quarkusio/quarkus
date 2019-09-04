package io.quarkus.it.dynamodb;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test connecting DynamoDB client to local Dynamodb.
 *
 */
@QuarkusTest
public class DynamoDbFunctionalityTest {

    @Test
    public void testDynamoDbAsync() {
        RestAssured.when().get("/test/async").then().body(is("OK"));
    }

    @Test
    public void testDynamoDbBlocking() {
        RestAssured.when().get("/test/blocking").then().body(is("OK"));
    }

}
