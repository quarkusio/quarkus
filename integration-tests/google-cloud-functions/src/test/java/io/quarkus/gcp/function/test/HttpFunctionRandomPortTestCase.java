package io.quarkus.gcp.function.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.google.cloud.functions.test.FunctionType;
import io.quarkus.google.cloud.functions.test.WithFunction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithFunction(FunctionType.HTTP)
class HttpFunctionRandomPortTestCase {

    @BeforeAll
    public static void setup() {
        System.setProperty("quarkus.http.test-port", "0");
    }

    @Test
    public void test() {
        // test the function using RestAssured
        when()
                .get()
                .then()
                .statusCode(200)
                .body(is("Hello World!"));
    }
}
