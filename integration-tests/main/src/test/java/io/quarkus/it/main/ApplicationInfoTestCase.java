package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ApplicationInfoTestCase {

    @Test
    public void testConfigPropertiesProperlyInjected() {
        RestAssured
                .when().get("/application-info")
                .then().body(is("main-integration-test/1.0"));
    }
}
