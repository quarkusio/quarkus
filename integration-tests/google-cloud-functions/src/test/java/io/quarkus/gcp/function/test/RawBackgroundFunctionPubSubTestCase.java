package io.quarkus.gcp.function.test;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import com.google.cloud.functions.invoker.runner.Invoker;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(RawBackgroundFunctionPubSubTestCase.RawBackgroundFunctionTestProfile.class)
class RawBackgroundFunctionPubSubTestCase {
    @Test
    public void test() throws Exception {
        // start the invoker without joining to avoid blocking the thread
        Invoker invoker = new Invoker(
                8081,
                "io.quarkus.gcp.functions.QuarkusBackgroundFunction",
                "event",
                Thread.currentThread().getContextClassLoader());
        invoker.startTestServer();

        // test the function using RestAssured
        given()
                .body("{\"data\":{\"name\":\"hello.txt\"}}")
                .when()
                .post("http://localhost:8081")
                .then()
                .statusCode(200);

        // stop the invoker
        invoker.stopServer();
    }

    public static class RawBackgroundFunctionTestProfile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "raw-background";
        }
    }
}
