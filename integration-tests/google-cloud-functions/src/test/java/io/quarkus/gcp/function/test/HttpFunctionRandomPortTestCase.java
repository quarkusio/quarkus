package io.quarkus.gcp.function.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.gcp.function.test.HttpFunctionRandomPortTestCase.RandomPort;
import io.quarkus.google.cloud.functions.test.FunctionType;
import io.quarkus.google.cloud.functions.test.WithFunction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(RandomPort.class)
@WithFunction(FunctionType.HTTP)
class HttpFunctionRandomPortTestCase {
    @Test
    public void test() {
        // test the function using RestAssured
        when()
                .get()
                .then()
                .statusCode(200)
                .body(is("Hello World!"));
    }

    public static class RandomPort implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.test-port", "0");
        }
    }
}
