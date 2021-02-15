package io.quarkus.it.keycloak;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@Disabled("Vert.x 4 Integration in progress - https://github.com/quarkusio/quarkus/issues/15084")
public class ServicePublicKeyTestCase {

    @Test
    public void testAccessTokenInjection() throws IOException, InterruptedException {
        String jwt = Jwt.claims().preferredUserName("alice").sign();
        Assertions.assertEquals("tenant-public-key:alice", RestAssured.given().auth()
                .oauth2(jwt)
                .get("/service/tenant-public-key").getBody().asString());
    }

    @Test
    public void testModifiedSignature() throws IOException, InterruptedException {
        String jwt = Jwt.claims().preferredUserName("alice").sign();
        // the last section of the jwt token is a signature
        Response r = RestAssured.given().auth()
                .oauth2(jwt + "1")
                .get("/service/tenant-public-key");
        Assertions.assertEquals(401, r.getStatusCode());
    }

    @Test
    public void testExpiredToken() throws IOException, InterruptedException {
        String jwt = Jwt.claims().preferredUserName("alice").expiresAt(Instant.EPOCH).sign();
        // the last section of the jwt token is a signature
        Response r = RestAssured.given().auth()
                .oauth2(jwt)
                .get("/service/tenant-public-key");
        Assertions.assertEquals(401, r.getStatusCode());
    }
}
