package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class DecoratorSelfInvocationTestCase {

    @Test
    public void testDecoratorDoesNotInterceptSelfInvocation() {
        // the concrete override must run exactly once; before the fix this returned a
        // StackOverflowError (HTTP 500) because the self-invocation re-entered the decorator chain
        RestAssured.when().get("/decorator-self-invocation").then()
                .statusCode(200)
                .body(is("1"));
    }
}
