package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MicroProfileConfigTestCase {

    public static final String HEADER_NAME = "header-name";

    @Test
    public void testMicroprofileConfigGetPropertyNames() {
        RestAssured
                .when().get("/microprofile-config/get-property-names")
                .then().body(is("OK"));
    }

    @Test
    public void testMicroprofileConfigGetCustomValue() {
        RestAssured
                .when().get("/microprofile-config/get-custom-value")
                .then().body(is("456"));
    }

    @Test
    public void testCidrAddress() {
        RestAssured
                .when().get("/microprofile-config/get-cidr-address")
                .then().body(is("10.0.0.0"));
    }
}
