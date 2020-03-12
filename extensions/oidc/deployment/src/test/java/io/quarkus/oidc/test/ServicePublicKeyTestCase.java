package io.quarkus.oidc.test;

import java.io.IOException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;

public class ServicePublicKeyTestCase {

    private static Class<?>[] testClasses = {
            ServiceProtectedResource.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("privateKey.pem")
                    .addAsResource("application-service-public-key.properties", "application.properties"));

    @Test
    public void testAccessTokenInjection() throws IOException, InterruptedException {
        String jwt = Jwt.claims().preferredUserName("alice").sign();
        Assertions.assertEquals("alice", RestAssured.given().auth()
                .oauth2(jwt)
                .get("/service").getBody().asString());
    }

    @Test
    public void testModifiedSignature() throws IOException, InterruptedException {
        String jwt = Jwt.claims().preferredUserName("alice").sign();
        // the last section of the jwt token is a signature
        Response r = RestAssured.given().auth()
                .oauth2(jwt + "1")
                .get("/service");
        Assertions.assertEquals(403, r.getStatusCode());
    }
}
