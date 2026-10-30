package io.quarkus.oidc.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * The default tenant which is not configured must be disabled when the {@link TenantConfigResolver} is registered.
 */
class UnconfiguredDefaultTenantDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TenantResource.class, NullTenantConfigResolver.class))
            .overrideConfigKey("quarkus.keycloak.devservices.enabled", "false")
            .overrideConfigKey("quarkus.oidc.devservices.enabled", "false")
            // the Keycloak test resource registered by other tests of this module configures the default tenant
            .overrideRuntimeConfigKey("quarkus.oidc.auth-server-url", "");

    @Test
    void testDefaultTenantDisabled() {
        given().get("/tenant/default").then().statusCode(200).body(is("ready=true,enabled=false"));
        // the disabled default tenant can not authenticate requests
        given().auth().oauth2("access-token").get("/tenant/protected").then().statusCode(401);
    }

    @Path("/tenant")
    public static class TenantResource {

        @Inject
        TenantConfigBean tenantConfigBean;

        @GET
        @Path("/default")
        public String defaultTenant() {
            var defaultTenant = tenantConfigBean.getDefaultTenant();
            return "ready=" + defaultTenant.ready() + ",enabled=" + defaultTenant.oidcConfig().tenantEnabled();
        }

        @Authenticated
        @GET
        @Path("/protected")
        public String protectedResource() {
            return "protected";
        }
    }

    @ApplicationScoped
    public static class NullTenantConfigResolver implements TenantConfigResolver {

        @Override
        public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
                OidcRequestContext<OidcTenantConfig> requestContext) {
            return Uni.createFrom().nullItem();
        }
    }
}
