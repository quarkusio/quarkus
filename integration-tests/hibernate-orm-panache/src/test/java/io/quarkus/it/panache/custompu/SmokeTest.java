package io.quarkus.it.panache.custompu;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class SmokeTest {

    @Test
    void testPanacheFunctionality() throws Exception {
        RestAssured.when().post("/custom-pu/someValue").then().body(containsString("someValue"));
        RestAssured.when().patch("/custom-pu/someUpdatedValue").then().body(containsString("someUpdatedValue"));
    }
}
