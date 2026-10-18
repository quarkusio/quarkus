package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
public class ServicePublicKeyTestCase {

    @Test
    public void testAccessTokenInjection() {
        String jwt = Jwt.claim("scope", "read:data").preferredUserName("alice").sign();
        assertEquals("tenant-public-key:alice", RestAssured.given().auth()
                .oauth2(jwt)
                .get("/service/tenant-public-key").getBody().asString());
    }

    @Test
    public void testAccessTokenInjection403() {
        String jwt = Jwt.claim("scope", "read:doc").preferredUserName("alice").sign();
        RestAssured.given().auth().oauth2(jwt)
                .get("/service/tenant-public-key").then().statusCode(403);
    }

    @Test
    public void testModifiedSignature() {
        String jwt = Jwt.claims().preferredUserName("alice").sign();
        // the last section of the jwt token is a signature
        Response r = RestAssured.given().auth()
                .oauth2(jwt + "1")
                .get("/service/tenant-public-key");
        assertEquals(401, r.getStatusCode());
    }

    @Test
    public void testExpiredToken() throws IOException, InterruptedException {
        String jwt = Jwt.claims().preferredUserName("alice").expiresAt(Instant.EPOCH).sign();
        // the last section of the jwt token is a signature
        Response r = RestAssured.given().auth()
                .oauth2(jwt)
                .get("/service/tenant-public-key");
        assertEquals(401, r.getStatusCode());
    }

    @Test
    public void testTokenIssuerVerification() {
        String jwt = Jwt.claim("scope", "read:data").preferredUserName("alice").issuer("acceptable-issuer").sign();
        assertEquals("tenant-public-key:alice", RestAssured.given().auth()
                .oauth2(jwt)
                .get("/service/tenant-public-key").getBody().asString());
        jwt = Jwt.claim("scope", "read:data").preferredUserName("alice").issuer("unacceptable-issuer").sign();
        RestAssured.given().auth()
                .oauth2(jwt)
                .get("/service/tenant-public-key")
                .then()
                .statusCode(401);
    }
}
