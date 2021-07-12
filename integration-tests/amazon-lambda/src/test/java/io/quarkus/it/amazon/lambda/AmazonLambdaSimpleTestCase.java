package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        resetCounter();
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stu");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Hello Stu"));
    }

    @Test
    public void testSimpleLambdaFailure() throws Exception {
        resetCounter();
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stuart");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(500)
                .body("errorMessage", equalTo(ProcessingService.CAN_ONLY_GREET_NICKNAMES));
        assertCounter(1);
    }

    private void resetCounter() {
        if (!isNativeImageTest()) {
            CounterInterceptor.COUNTER.set(0);
        }
    }

    private void assertCounter(int expected) {
        if (!isNativeImageTest()) {
            assertEquals(expected, CounterInterceptor.COUNTER.get());
        }
    }

    private boolean isNativeImageTest() {
        return System.getProperty("native.image.path") != null;
    }
}
