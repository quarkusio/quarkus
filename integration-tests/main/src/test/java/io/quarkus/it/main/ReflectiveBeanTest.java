package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class ReflectiveBeanTest {

    @Test
    public void testReflectiveAccess() {
        RestAssured.when().get("/reflective-bean/proxy").then()
                .body(is("42"));
        RestAssured.when().get("/reflective-bean/subclass").then()
                .body(is("42"));
    }

}
