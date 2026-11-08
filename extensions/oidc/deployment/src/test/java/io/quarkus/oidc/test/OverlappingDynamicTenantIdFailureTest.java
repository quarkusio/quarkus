package io.quarkus.oidc.test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * The dynamic tenant `tenant-a_1` can not be created because the cookies of the static web-app tenant `tenant-a`
 * with a cookie suffix `1` would be resolved to the tenant `tenant-a_1`.
 */
public class OverlappingDynamicTenantIdFailureTest {

    private static final String EXPECTED_MESSAGE = "Tenant id 'tenant-a_1' starts with the id of the tenant 'tenant-a' followed by '_'";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ProtectedResource.class, OverlappingDynamicTenantConfigResolver.class))
            .overrideConfigKey("quarkus.keycloak.devservices.enabled", "false")
            .overrideConfigKey("quarkus.oidc.devservices.enabled", "false")
            // the Keycloak test resource registered by other tests of this module configures the default tenant
            .overrideRuntimeConfigKey("quarkus.oidc.auth-server-url", "")
            .overrideConfigKey("quarkus.oidc.tenant-a.auth-server-url", "http://localhost/oidc")
            .overrideConfigKey("quarkus.oidc.tenant-a.discovery-enabled", "false")
            .overrideConfigKey("quarkus.oidc.tenant-a.authorization-path", "/authorize")
            .overrideConfigKey("quarkus.oidc.tenant-a.token-path", "/token")
            .overrideConfigKey("quarkus.oidc.tenant-a.application-type", "web-app")
            .overrideConfigKey("quarkus.oidc.tenant-a.client-id", "my-client")
            .setLogRecordPredicate(record -> record.getThrown() != null)
            .assertLogRecords(records -> assertTrue(
                    records.stream().anyMatch(OverlappingDynamicTenantIdFailureTest::hasExpectedCause),
                    "Expected a logged failure containing: " + EXPECTED_MESSAGE));

    @Test
    public void testDynamicTenantIsNotCreated() {
        // the static tenant `tenant-a` is resolved from the path and challenges the user
        given().redirects().follow(false).get("/tenant-a/protected").then().statusCode(302);
        // the dynamic tenant `tenant-a_1` can not be created
        given().redirects().follow(false).get("/tenant-a_1/protected").then().statusCode(500);
    }

    private static boolean hasExpectedCause(LogRecord record) {
        Throwable t = record.getThrown();
        while (t != null) {
            if (t.getMessage() != null && t.getMessage().contains(EXPECTED_MESSAGE)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    @Path("/")
    public static class ProtectedResource {

        @Authenticated
        @GET
        @Path("{tenant}/protected")
        public String protectedResource() {
            return "protected";
        }
    }

    @ApplicationScoped
    public static class OverlappingDynamicTenantConfigResolver implements TenantConfigResolver {

        @Override
        public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
            if (context.request().path().startsWith("/tenant-a_1/")) {
                return Uni.createFrom().item(OidcTenantConfig.builder()
                        .tenantId("tenant-a_1")
                        .authServerUrl("http://localhost/oidc")
                        .discoveryEnabled(false)
                        .authorizationPath("/authorize")
                        .tokenPath("/token")
                        .clientId("my-client")
                        .applicationType(ApplicationType.WEB_APP)
                        .build());
            }
            return Uni.createFrom().nullItem();
        }
    }
}
