package io.quarkus.it.bouncycastle;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class BouncyCastleTestCase {

    @Test
    public void testListProviders() {
        RestAssured.given()
                .when()
                .get("/jca/listProviders")
                .then()
                .statusCode(200)
                .body(equalTo("BC"));
    }

    @Test
    public void testSHA256withRSAandMGF1() {
        RestAssured.given()
                .when()
                .get("/jca/SHA256withRSAandMGF1")
                .then()
                .statusCode(200)
                .body(equalTo("success"));
    }

    @Test
    public void testGenerateEcKeyPair() {
        RestAssured.given()
                .when()
                .get("/jca/generateEcKeyPair")
                .then()
                .statusCode(200)
                .body(equalTo("success"));
    }

    @Test
    public void testGenerateRsaKeyPair() {
        RestAssured.given()
                .when()
                .get("/jca/generateRsaKeyPair")
                .then()
                .statusCode(200)
                .body(equalTo("success"));
    }

}
