package io.quarkus.oidc.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.jwt.build.Jwt;

class StepUpDecryptAccessTokenTest {

    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlivFI8qB4D0y2jy0CfEqFyy46R0o7S8TKpsx5xbHKoU1VWg6QkQm+ntyIv1p4kE1sPEQO73+HY8+Bzs75XwRTYL1BmR1w8J5hmjVWjc6R2BTBGAYRPFRhor3kpM6ni2SPmNNhurEAHw7TaqszP5eUF/F9+KEBWkwVta+PZ37bwqSE4sCb1soZFrVz/UT/LF4tYpuVYt3YbqToZ3pZOZ9AX2o1GCG3xwOjkc4x0W7ezbQZdC9iftPxVHR8irOijJRRjcPDtA6vPKpzLl6CyYnsIYPd99ltwxTHjr3npfv/3Lw50bAkbT4HeLFxTx4flEoZLKO/g0bAoV2uqBhkA9xnQIDAQAB";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(StepUpResource.class)
                    .addAsResource("privateKey.pem"))
            .withConfiguration("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.oidc.devservices.enabled=false
                    quarkus.http.auth.proactive=false
                    """)
            .withRuntimeConfiguration("""
                    quarkus.oidc.application-type=service
                    quarkus.oidc.auth-server-url=
                    quarkus.oidc.client-id=test
                    quarkus.oidc.public-key=%s
                    quarkus.oidc.token.issuer=step-up-issuer
                    quarkus.oidc.token.decrypt-access-token=true
                    """.formatted(PUBLIC_KEY));

    @Test
    void bronzeTokenRemainsValidOnAnUnrestrictedPath() {
        given().auth().oauth2(token("bronze"))
                .get("/plain")
                .then().statusCode(200).body(is("plain"));
    }

    @Test
    void bronzeTokenStillRequiresStepUpWhenDecryptionIsEnabled() {
        given().auth().oauth2(token("bronze"))
                .get("/gold")
                .then().statusCode(401)
                .header("www-authenticate", containsString("insufficient_user_authentication"))
                .header("www-authenticate", containsString("acr_values=\"gold\""));
    }

    @Test
    void goldTokenPassesStepUpWhenDecryptionIsEnabled() {
        given().auth().oauth2(token("gold"))
                .get("/gold")
                .then().statusCode(200).body(is("gold"));
    }

    private static String token(String acr) {
        return Jwt.preferredUserName("alice").issuer("step-up-issuer").claim("acr", acr).sign("/privateKey.pem");
    }

    @BearerTokenAuthentication
    @Path("/")
    public static class StepUpResource {

        @GET
        @Path("plain")
        public String plain() {
            return "plain";
        }

        @AuthenticationContext("gold")
        @GET
        @Path("gold")
        public String gold() {
            return "gold";
        }
    }
}
