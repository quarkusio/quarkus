package io.quarkus.funqy.gcp.functions.test;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import com.google.cloud.functions.invoker.runner.Invoker;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(GreetingFunctionsPubsubTest.PubsubFunctionTestProfile.class)
class GreetingFunctionsPubsubTest {
    @Test
    public void test() throws Exception {
        // start the invoker without joining to avoid blocking the thread
        Invoker invoker = new Invoker(
                8081,
                "io.quarkus.funqy.gcp.functions.FunqyBackgroundFunction",
                "event",
                Thread.currentThread().getContextClassLoader());
        invoker.startTestServer();

        // test the function using RestAssured
        given()
                .body("{\"data\":{\"data\":\"world\"}}")
                .when()
                .post("http://localhost:8081")
                .then()
                .statusCode(200);

        // stop the invoker
        invoker.stopServer();
    }

    public static class PubsubFunctionTestProfile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "pubsub";
        }
    }
}
