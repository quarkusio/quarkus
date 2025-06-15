package io.quarkus.funqy.gcp.functions.test;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.google.cloud.functions.test.FunctionType;
import io.quarkus.google.cloud.functions.test.WithFunction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithFunction(value = FunctionType.FUNQY_BACKGROUND, functionName = "helloPubSubWorld")
class GreetingFunctionsPubsubTest {
    @Test
    public void test() {
        // test the function using RestAssured
        given().body("{\"data\":{\"data\":\"world\"}}").when().post().then().statusCode(200);
    }
}
