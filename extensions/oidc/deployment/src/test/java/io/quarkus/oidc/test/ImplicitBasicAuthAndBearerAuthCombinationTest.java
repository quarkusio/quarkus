package io.quarkus.oidc.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.restassured.RestAssured;

@WithTestResource(value = KeycloakTestResourceLifecycleManager.class, restrictToAnnotatedClass = false)
public class ImplicitBasicAuthAndBearerAuthCombinationTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BasicBearerResource.class)
                    .addAsResource(
                            new StringAsset("""
                                    quarkus.security.users.embedded.enabled=true
                                    quarkus.security.users.embedded.plain-text=true
                                    quarkus.security.users.embedded.users.alice=alice
                                    quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus
                                    quarkus.oidc.client-id=quarkus-service-app
                                    quarkus.oidc.credentials.secret=secret
                                    quarkus.http.auth.proactive=false
                                    """),
                            "application.properties"));

    @Test
    public void testBasicEnabledAsSelectedWithAnnotation() {
        // endpoint is annotated with 'BasicAuthentication', so basic auth must be enabled
        RestAssured.given().auth().oauth2(getAccessToken()).get("/basic-bearer/bearer")
                .then().statusCode(200).body(Matchers.is("alice"));
        RestAssured.given().auth().basic("alice", "alice").get("/basic-bearer/basic")
                .then().statusCode(204);
        RestAssured.given().auth().basic("alice", "alice").get("/basic-bearer/bearer")
                .then().statusCode(401);
        RestAssured.given().auth().oauth2(getAccessToken()).get("/basic-bearer/basic")
                .then().statusCode(401);
    }

    private static String getAccessToken() {
        return KeycloakTestResourceLifecycleManager.getAccessToken("alice");
    }

    @BearerTokenAuthentication
    @Path("basic-bearer")
    public static class BasicBearerResource {

        @Inject
        JsonWebToken accessToken;

        @GET
        @BasicAuthentication
        @Path("basic")
        public String basic() {
            return accessToken.getName();
        }

        @GET
        @Path("bearer")
        public String bearer() {
            return accessToken.getName();
        }
    }

}
