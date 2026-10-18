package io.quarkus.oidc.test;

import static io.quarkus.oidc.runtime.OidcUtils.TENANT_ID_ATTRIBUTE;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;
import io.vertx.ext.web.RoutingContext;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class OidcIssuerBasedTenantResolverTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BearerResource.class, ResponseData.class)
                    .addAsResource("privateKey.pem")
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.oidc.tenant-keycloak.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.tenant-keycloak.client-id=quarkus-service-app
                                            quarkus.oidc.tenant-keycloak.credentials.secret=secret
                                            quarkus.oidc.resolve-tenants-with-issuer=true
                                            quarkus.keycloak.devservices.enabled=false
                                            quarkus.oidc.tenant-public-key.client-id=test
                                            quarkus.oidc.tenant-public-key.token.issuer=my_issuer
                                            quarkus.oidc.tenant-public-key.public-key=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlivFI8qB4D0y2jy0CfEqFyy46R0o7S8TKpsx5xbHKoU1VWg6QkQm+ntyIv1p4kE1sPEQO73+HY8+Bzs75XwRTYL1BmR1w8J5hmjVWjc6R2BTBGAYRPFRhor3kpM6ni2SPmNNhurEAHw7TaqszP5eUF/F9+KEBWkwVta+PZ37bwqSE4sCb1soZFrVz/UT/LF4tYpuVYt3YbqToZ3pZOZ9AX2o1GCG3xwOjkc4x0W7ezbQZdC9iftPxVHR8irOijJRRjcPDtA6vPKpzLl6CyYnsIYPd99ltwxTHjr3npfv/3Lw50bAkbT4HeLFxTx4flEoZLKO/g0bAoV2uqBhkA9xnQIDAQAB
                                            """),
                            "application.properties"));

    @Test
    void testKeycloakTenantResolved() {
        var token = KeycloakTestResourceLifecycleManager.getAccessToken("alice");
        RestAssured.given().auth().oauth2(token).get("/bearer/public-key")
                .then().statusCode(200)
                .body("principalName", Matchers.is("alice"))
                .body("tenantId", Matchers.is("tenant-keycloak"));
    }

    @Test
    void testPublicKeyTenantResolved() {
        // known issuer, hence the correct OIDC tenant must be resolved
        var token = Jwt.preferredUserName("zeus").issuer("my_issuer").sign("/privateKey.pem");
        RestAssured.given().auth().oauth2(token).get("/bearer/keycloak")
                .then().statusCode(200)
                .body("principalName", Matchers.is("zeus"))
                .body("tenantId", Matchers.is("tenant-public-key"));
        // wrong issuer, hence the correct OIDC tenant wasn't resolved
        token = Jwt.preferredUserName("zeus").issuer("wrong_issuer").sign("/privateKey.pem");
        RestAssured.given().auth().oauth2(token).get("/bearer/keycloak")
                .then().statusCode(401);
    }

    public record ResponseData(String tenantId, String principalName) {
    }

    @Path("bearer")
    public static class BearerResource {

        @Inject
        JsonWebToken accessToken;

        @Inject
        RoutingContext routingContext;

        @GET
        @Path("public-key")
        public ResponseData publicKeyTenant() {
            return getResponseData();
        }

        @GET
        @Path("keycloak")
        public ResponseData keycloakTenant() {
            return getResponseData();
        }

        private ResponseData getResponseData() {
            return new ResponseData(routingContext.get(TENANT_ID_ATTRIBUTE), accessToken.getName());
        }
    }

}
