package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ConfigPropertiesTestCase {

    @Test
    public void testConfigPropertiesProperlyInjected() {
        RestAssured
                .when().get("/configuration-properties")
                .then().body(is("HelloONE!"));
    }

    @Test
    public void testImplicitConverters() {
        RestAssured
                .when().get("/configuration-properties/period")
                .then().body(is("P1D"));
    }
}
