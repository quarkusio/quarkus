package org.acme.funqygooglecloudfunctions;

import io.quarkus.google.cloud.functions.test.FunctionType;
import io.quarkus.google.cloud.functions.test.WithFunction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@WithFunction(value = FunctionType.FUNQY_BACKGROUND, functionName = "helloPubSubWorld")
class GreetingFunctionsPubSubTest {
    @Test
    void testHelloPubSubWorld() {
        given()
                .body("{\"data\":{\"data\":\"world\"}}")
                .when()
                .post()
                .then()
                .statusCode(200);
    }
}