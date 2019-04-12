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

    protected String asyncTable() {
        return "async";
    }

    protected String blockingTable() {
        return "blocking";
    }

    @Test
    public void testDynamoDbAsync() {
        RestAssured.given().queryParam("table", asyncTable()).when().get("/test/async").then().body(is("OK"));
    }

    @Test
    public void testDynamoDbBlocking() {
        RestAssured.given().queryParam("table", blockingTable()).when().get("/test/blocking?table=blocking").then()
                .body(is("OK"));
    }

}
