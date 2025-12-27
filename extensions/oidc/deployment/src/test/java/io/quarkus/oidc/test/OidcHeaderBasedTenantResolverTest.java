package io.quarkus.oidc.test;

import static io.quarkus.oidc.runtime.OidcUtils.TENANT_ID_ATTRIBUTE;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.ext.web.RoutingContext;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
class OidcHeaderBasedTenantResolverTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BearerResource.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.oidc.tenant-a.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.tenant-a.client-id=quarkus-service-app
                                            quarkus.oidc.tenant-a.credentials.secret=secret
                                            quarkus.oidc.tenant-a.token.header=Custom-Authorization-1
                                            quarkus.oidc.tenant-b.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.tenant-b.client-id=quarkus-service-app
                                            quarkus.oidc.tenant-b.credentials.secret=secret
                                            quarkus.oidc.tenant-b.token.header=Custom-Authorization-2
                                            quarkus.keycloak.devservices.enabled=false
                                            """),
                            "application.properties"));

    @Test
    void testTenantAResolvedUsingHeader() {
        assertTenantResolverForHeader("Custom-Authorization-1", "tenant-a");
    }

    @Test
    void testTenantBResolvedUsingHeader() {
        assertTenantResolverForHeader("Custom-Authorization-2", "tenant-b");
    }

    @Test
    void testWhenHeaderNotMatchedDefaultTenantIsUsed() {
        assertTenantResolverForHeader("Authorization", "Default");
    }

    private static void assertTenantResolverForHeader(String customAuthorizationHeader, String tenantId) {
        var token = KeycloakTestResourceLifecycleManager.getAccessToken("alice");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .accept(ContentType.TEXT)
                .header(customAuthorizationHeader, "Bearer " + token)
                .get("/bearer/tenant")
                .then().statusCode(200)
                .body(Matchers.is(tenantId));
    }

    @Path("bearer")
    public static class BearerResource {

        @Inject
        RoutingContext routingContext;

        @Authenticated
        @GET
        @Path("tenant")
        public String getTenant() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }

    }

}
