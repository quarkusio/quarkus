package io.quarkus.it.elytron;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JCATestCase {

    @Test
    public void testListProviders() {
        RestAssured.given()
                .when()
                .get("/jca/listProviders")
                .then()
                .statusCode(200)
                .body(containsString("SunRsaSign"));
    }

    @Test
    public void testDecodeRSAKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        byte[] encoded = publicKey.getEncoded();
        byte[] pemEncoded = Base64.getEncoder().encode(encoded);
        String pemString = new String(pemEncoded, "UTF-8");

        RestAssured.given()
                .queryParam("pemEncoded", pemString)
                .when()
                .get("/jca/decodeRSAKey")
                .then()
                .statusCode(200)
                .body(is("RSA"));
    }

    @Test
    public void testVerifyRSASig() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        byte[] encoded = publicKey.getEncoded();
        byte[] pemEncoded = Base64.getEncoder().encode(encoded);
        String pemString = new String(pemEncoded, "UTF-8");

        RestAssured.given()
                .queryParam("msg", "Hello verifyRSASig")
                .queryParam("publicKey", pemString)
                .queryParam("sig", "")
                .when()
                .get("/jca/verifyRSASig")
                .then()
                .statusCode(200)
                .body(is("true"));
    }
}
