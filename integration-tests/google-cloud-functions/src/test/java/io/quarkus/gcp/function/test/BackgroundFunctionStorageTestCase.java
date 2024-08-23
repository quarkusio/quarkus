package io.quarkus.gcp.function.test;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.google.cloud.functions.test.FunctionType;
import io.quarkus.google.cloud.functions.test.WithFunction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithFunction(value = FunctionType.BACKGROUND, functionName = "storageTest")
class BackgroundFunctionStorageTestCase {
    @Test
    public void test() {
        // test the function using RestAssured
        given()
                .body("{\"data\":{\"name\":\"hello.txt\"}}")
                .when()
                .post()
                .then()
                .statusCode(200);
    }
}
