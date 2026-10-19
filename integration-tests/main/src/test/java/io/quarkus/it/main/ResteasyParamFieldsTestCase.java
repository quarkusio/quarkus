package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ResteasyParamFieldsTestCase {

    @Test
    public void testFieldsAreInjected() {
        RestAssured.given().queryParam("queryField", "q")
                .when().get("/param-fields/p/pp")
                .then().statusCode(200).body(is("p:q"));
    }
}
