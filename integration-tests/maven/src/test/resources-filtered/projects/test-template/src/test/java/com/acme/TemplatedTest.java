package com.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TemplatedTest {

    @TestTemplate
    @ExtendWith(UserIdGeneratorTestInvocationContextProvider.class)
    public void testHelloEndpoint(UserIdGeneratorTestCase testCase) {
        given()
                .when().get("/app/hello")
                .then()
                .statusCode(200)
                .body(is(testCase.getExpectedBody()));
    }
}
