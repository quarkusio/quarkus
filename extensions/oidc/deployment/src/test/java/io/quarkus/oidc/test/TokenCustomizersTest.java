package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.Oidc;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfigBuilder;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TokenCustomizer;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.vertx.ext.web.RoutingContext;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class TokenCustomizersTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AccessTokenResource.class, GlobalTokenCustomizer.class, AccessTokenResource.Customizers.class,
                            NamedOneAndTwoCustomizer.class, NamedOneAndTwoAndFourCustomizer.class));

    @Test
    public void testTokenCustomizers() {
        RestAssured.given().auth().oauth2(getAccessToken()).get("/access-token")
                .then().statusCode(200).body(Matchers.is("default"));
        var customizers = RestAssured.get("/access-token/customizers").then().statusCode(200).extract()
                .as(AccessTokenResource.Customizers.class);
        assertTrue(customizers.global);
        assertFalse(customizers.namedOneAndTwoCustomizer);
        assertFalse(customizers.namedOneAndTwoAndFourCustomizer);
        RestAssured.given().auth().oauth2(getAccessToken()).get("/access-token/named-1")
                .then().statusCode(200).body(Matchers.is("named-1"));
        customizers = RestAssured.get("/access-token/customizers").then().statusCode(200).extract()
                .as(AccessTokenResource.Customizers.class);
        assertTrue(customizers.global);
        assertTrue(customizers.namedOneAndTwoCustomizer);
        assertTrue(customizers.namedOneAndTwoAndFourCustomizer);
        RestAssured.given().auth().oauth2(getAccessToken()).get("/access-token/named-2")
                .then().statusCode(200).body(Matchers.is("named-2"));
        customizers = RestAssured.get("/access-token/customizers").then().statusCode(200).extract()
                .as(AccessTokenResource.Customizers.class);
        assertTrue(customizers.global);
        assertTrue(customizers.namedOneAndTwoCustomizer);
        assertTrue(customizers.namedOneAndTwoAndFourCustomizer);
        RestAssured.given().auth().oauth2(getAccessToken()).get("/access-token/named-3")
                .then().statusCode(200).body(Matchers.is("named-3"));
        customizers = RestAssured.get("/access-token/customizers").then().statusCode(200).extract()
                .as(AccessTokenResource.Customizers.class);
        assertTrue(customizers.global);
        assertFalse(customizers.namedOneAndTwoCustomizer);
        assertFalse(customizers.namedOneAndTwoAndFourCustomizer);
        RestAssured.given().auth().oauth2(getAccessToken()).get("/access-token/named-4")
                .then().statusCode(200).body(Matchers.is("named-4"));
        customizers = RestAssured.get("/access-token/customizers").then().statusCode(200).extract()
                .as(AccessTokenResource.Customizers.class);
        assertTrue(customizers.global);
        assertFalse(customizers.namedOneAndTwoCustomizer);
        assertTrue(customizers.namedOneAndTwoAndFourCustomizer);
    }

    private static String getAccessToken() {
        return KeycloakTestResourceLifecycleManager.getAccessToken("alice");
    }

    @Unremovable
    @Singleton
    public static class GlobalTokenCustomizer implements TokenCustomizer {

        volatile boolean called = false;

        @Override
        public JsonObject customizeHeaders(JsonObject headers) {
            called = true;
            return null;
        }
    }

    @Unremovable
    @Singleton
    @TenantFeature({ "named-1", "named-2" })
    public static class NamedOneAndTwoCustomizer implements TokenCustomizer {

        volatile boolean called = false;

        @Override
        public JsonObject customizeHeaders(JsonObject headers) {
            called = true;
            return null;
        }
    }

    @Unremovable
    @Singleton
    @TenantFeature({ "named-1", "named-2", "named-4" })
    public static class NamedOneAndTwoAndFourCustomizer implements TokenCustomizer {

        volatile boolean called = false;

        @Override
        public JsonObject customizeHeaders(JsonObject headers) {
            called = true;
            return null;
        }
    }

    @Path("/access-token")
    public static class AccessTokenResource {

        private GlobalTokenCustomizer globalTokenCustomizer;
        private NamedOneAndTwoCustomizer namedOneAndTwoCustomizer;
        private NamedOneAndTwoAndFourCustomizer namedOneAndTwoAndFourCustomizer;

        @Inject
        RoutingContext context;

        public AccessTokenResource(@All List<TokenCustomizer> tokenCustomizers) {
            for (TokenCustomizer tokenCustomizer : tokenCustomizers) {
                if (tokenCustomizer instanceof GlobalTokenCustomizer i) {
                    globalTokenCustomizer = i;
                } else if (tokenCustomizer instanceof NamedOneAndTwoCustomizer i) {
                    namedOneAndTwoCustomizer = i;
                } else if (tokenCustomizer instanceof NamedOneAndTwoAndFourCustomizer i) {
                    namedOneAndTwoAndFourCustomizer = i;
                }
            }
        }

        @Authenticated
        @GET
        public String defaultTenantAccessTokenName() {
            return "default";
        }

        @Authenticated
        @Path("/{tenant-id}")
        @GET
        public String namedTenantAccessTokenName() {
            return context.<OidcTenantConfig> get(OidcTenantConfig.class.getName()).tenantId().get();
        }

        record Customizers(boolean global, boolean namedOneAndTwoCustomizer, boolean namedOneAndTwoAndFourCustomizer) {
        }

        @Path("/customizers")
        @GET
        public Customizers customizers() {
            try {
                return new Customizers(globalTokenCustomizer.called, namedOneAndTwoCustomizer.called,
                        namedOneAndTwoAndFourCustomizer.called);
            } finally {
                globalTokenCustomizer.called = false;
                namedOneAndTwoCustomizer.called = false;
                namedOneAndTwoAndFourCustomizer.called = false;
            }
        }

        void configureOidc(@Observes Oidc oidc, OidcConfig oidcConfig) {
            var defaultTenantConfigBuilder = new OidcTenantConfigBuilder(OidcConfig.getDefaultTenant(oidcConfig));
            oidc.create(defaultTenantConfigBuilder.tenantId("named-1").build());
            oidc.create(defaultTenantConfigBuilder.tenantId("named-2").build());
            oidc.create(defaultTenantConfigBuilder.tenantId("named-3").build());
            oidc.create(defaultTenantConfigBuilder.tenantId("named-4").build());
        }
    }

}
