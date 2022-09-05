package io.quarkus.gcp.function.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.google.cloud.functions.invoker.runner.Invoker;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HttpFunctionTestCase {
    @Test
    public void test() throws Exception {
        // start the invoker without joining to avoid blocking the thread
        Invoker invoker = new Invoker(
                8081,
                "io.quarkus.gcp.functions.QuarkusHttpFunction",
                "http",
                Thread.currentThread().getContextClassLoader());
        invoker.startTestServer();

        // test the function using RestAssured
        when()
                .get("http://localhost:8081")
                .then()
                .statusCode(200)
                .body(is("Hello World!"));

        // stop the invoker
        invoker.stopServer();
    }
}
