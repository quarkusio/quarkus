package io.quarkus.it.dynamodb;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Disabled;
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
    @Disabled("Until we have a compatible version of the AWS SDK with the Netty version used in Quarkus, "
            + "disable the async support.")
    public void testDynamoDbAsync() {
        RestAssured.when().get("/test/async").then().body(is("OK"));
    }

    @Test
    public void testDynamoDbBlocking() {
        RestAssured.when().get("/test/blocking").then().body(is("OK"));
    }

}
