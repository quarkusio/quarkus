package io.quarkus.oidc.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.runtime.OidcTenantConfig.Provider;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

class TenantConfigResolverWellKnownProviderTest {

    private static final String TENANT_ID = "microsoft";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ProtectedResource.class, MicrosoftTenantConfigResolver.class,
                    MockDiscoveryEndpoint.class, MockDiscoveryWithoutUserInfoEndpoint.class, MockJwksEndpoint.class,
                    MockIntrospectionEndpoint.class, MockUserInfoEndpoint.class))
            .withConfiguration("""
                    quarkus.keycloak.devservices.enabled=false
                    quarkus.oidc.tenant-enabled=false
                    """);

    @Test
    void testTenantContextUpdatedOnlyForNewTenantConfig() {
        String tenantContext = getTenantContext(null, false);
        // the same tenant config instance returned by the TenantConfigResolver must not update the tenant context
        assertEquals(tenantContext, getTenantContext(null, false));

        // a new tenant config instance updates the tenant context without reconnecting to the provider
        String updatedTenantContext = getTenantContext("updated", false);
        assertNotEquals(tenantContext, updatedTenantContext);
        assertEquals(updatedTenantContext, getTenantContext(null, false));

        // the tenant context is replaced when requested
        String replacedTenantContext = getTenantContext("copy", true);
        assertNotEquals(updatedTenantContext, replacedTenantContext);
        assertEquals(replacedTenantContext, getTenantContext(null, false));

        // the replaced tenant context must not require UserInfo when the new provider does not support it
        String tenantContextWithoutUserInfo = getTenantContext("without-userinfo", true);
        assertNotEquals(replacedTenantContext, tenantContextWithoutUserInfo);
        assertEquals(tenantContextWithoutUserInfo, getTenantContext(null, false));
    }

    private static String getTenantContext(String newTenantConfig, boolean replaceTenantContext) {
        var request = given().auth().oauth2("opaque-token").queryParam("replace-tenant-context", replaceTenantContext);
        if (newTenantConfig != null) {
            request = request.queryParam("new-tenant-config", newTenantConfig);
        }
        return request.get("/protected").then().statusCode(200).extract().asString();
    }

    @Path("/protected")
    @Authenticated
    public static class ProtectedResource {

        @Inject
        TenantConfigBean tenantConfigBean;

        @Inject
        MicrosoftTenantConfigResolver tenantConfigResolver;

        @Inject
        UserInfo userInfo;

        @GET
        public String get() {
            var tenantContext = tenantConfigBean.getDynamicTenant(TENANT_ID);
            OidcTenantConfig resolverConfig = tenantConfigResolver.getTenantConfig();
            if (!tenantContext.isCreatedFrom(resolverConfig)) {
                throw new IllegalStateException(
                        "Tenant context must be created from the tenant config returned by the TenantConfigResolver");
            }
            if (resolverConfig.authentication().userInfoRequired().isPresent()) {
                throw new IllegalStateException("Tenant config returned by the TenantConfigResolver must not be modified");
            }
            OidcTenantConfig tenantConfig = tenantContext.getOidcTenantConfig();
            if (!"any".equals(tenantConfig.token().issuer().orElse(null))) {
                throw new IllegalStateException("Microsoft provider config must be merged into the tenant config");
            }
            boolean userInfoRequired = tenantConfig.authentication().userInfoRequired().orElse(false);
            boolean userInfoSupported = tenantContext.getOidcMetadata().getUserInfoUri() != null;
            if (userInfoRequired != userInfoSupported) {
                throw new IllegalStateException(
                        "UserInfo must be required only when it is injected and the provider supports it");
            }
            if (userInfoRequired != "test-user".equals(userInfo.getSubject())) {
                throw new IllegalStateException("UserInfo must be available only when it is required");
            }
            return String.valueOf(System.identityHashCode(tenantContext));
        }
    }

    @ApplicationScoped
    public static class MicrosoftTenantConfigResolver implements TenantConfigResolver {

        private volatile OidcTenantConfig tenantConfig = tenantConfig("http://localhost:8081/mock-oidc");

        @Override
        public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
                OidcRequestContext<OidcTenantConfig> requestContext) {
            if (!routingContext.normalizedPath().startsWith("/protected")) {
                return Uni.createFrom().nullItem();
            }
            String newTenantConfig = routingContext.request().getParam("new-tenant-config");
            if ("updated".equals(newTenantConfig)) {
                tenantConfig = OidcTenantConfig.builder(tenantConfig).clientName("updated-client-name").build();
            } else if ("copy".equals(newTenantConfig)) {
                tenantConfig = OidcTenantConfig.builder(tenantConfig).build();
            } else if ("without-userinfo".equals(newTenantConfig)) {
                // the current tenant config does not require UserInfo, only the tenant context created from it does
                tenantConfig = OidcTenantConfig.builder(tenantConfig)
                        .authServerUrl("http://localhost:8081/mock-oidc-without-userinfo").build();
            }
            if (Boolean.parseBoolean(routingContext.request().getParam("replace-tenant-context"))) {
                routingContext.put("replace-tenant-configuration-context", "true");
            }
            return Uni.createFrom().item(tenantConfig);
        }

        public OidcTenantConfig getTenantConfig() {
            return tenantConfig;
        }

        private static OidcTenantConfig tenantConfig(String authServerUrl) {
            return OidcTenantConfig.builder()
                    .tenantId(TENANT_ID)
                    .provider(Provider.MICROSOFT)
                    .authServerUrl(authServerUrl)
                    .applicationType(ApplicationType.SERVICE)
                    .clientId("quarkus-app")
                    .credentials("secret")
                    .build();
        }
    }

    @Path("/mock-oidc/.well-known/openid-configuration")
    public static class MockDiscoveryEndpoint {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public String discover() {
            return """
                    {
                        "issuer": "http://localhost:8081/mock-oidc",
                        "jwks_uri": "http://localhost:8081/mock-oidc/jwks",
                        "introspection_endpoint": "http://localhost:8081/mock-oidc/introspect",
                        "userinfo_endpoint": "http://localhost:8081/mock-oidc/userinfo"
                    }
                    """;
        }
    }

    @Path("/mock-oidc-without-userinfo/.well-known/openid-configuration")
    public static class MockDiscoveryWithoutUserInfoEndpoint {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public String discover() {
            return """
                    {
                        "issuer": "http://localhost:8081/mock-oidc-without-userinfo",
                        "jwks_uri": "http://localhost:8081/mock-oidc/jwks",
                        "introspection_endpoint": "http://localhost:8081/mock-oidc/introspect"
                    }
                    """;
        }
    }

    @Path("/mock-oidc/userinfo")
    public static class MockUserInfoEndpoint {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public String userInfo() {
            return "{\"sub\":\"test-user\"}";
        }
    }

    @Path("/mock-oidc/jwks")
    public static class MockJwksEndpoint {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public String jwks() {
            return "{\"keys\":[]}";
        }
    }

    @Path("/mock-oidc/introspect")
    public static class MockIntrospectionEndpoint {

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        public String introspect() {
            return "{\"active\":true,\"sub\":\"test-user\",\"username\":\"test-user\"}";
        }
    }
}
