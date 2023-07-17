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
                .body(equalTo("SunPKCS11,BC"));
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
    public void testGenerateEcDsaKeyPair() {
        RestAssured.given()
                .when()
                .get("/jca/generateEcDsaKeyPair")
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

    @Test
    public void testAesCbcPKCS7PaddingCipher() {
        RestAssured.given()
                .when()
                .get("/jca/checkAesCbcPKCS7PaddingCipher")
                .then()
                .statusCode(200)
                .body(equalTo("AES/CBC/PKCS7Padding"));
    }

    @Test
    public void readEcPrivatePemKey() {
        RestAssured.given()
                .when()
                .get("/jca/readEcPrivatePemKey")
                .then()
                .statusCode(200)
                .body(equalTo("success"));
    }

    @Test
    public void readRsaPrivatePemKey() {
        RestAssured.given()
                .when()
                .get("/jca/readRsaPrivatePemKey")
                .then()
                .statusCode(200)
                .body(equalTo("success"));
    }
}
