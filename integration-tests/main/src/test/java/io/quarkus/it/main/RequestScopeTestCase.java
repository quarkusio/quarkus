package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class RequestScopeTestCase {

    @Test
    public void testRequestScope() {
        RestAssured.when().get("/request-scoped").then()
                .body(is("3"));

        RestAssured.when().get("/request-scoped").then()
                .body(is("3"));
    }

}
