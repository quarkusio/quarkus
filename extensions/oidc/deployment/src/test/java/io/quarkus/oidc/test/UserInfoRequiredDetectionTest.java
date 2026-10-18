package io.quarkus.oidc.test;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.Oidc;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class UserInfoRequiredDetectionTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UserInfoResource.class, UserInfoEndpoint.class, OidcStartup.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            # Disable Dev Services, we use a test resource manager
                                            quarkus.keycloak.devservices.enabled=false

                                            quarkus.oidc.tenant-paths=/user-info/default-tenant-random
                                            quarkus.oidc.user-info-path=http://${quarkus.http.host}:${quarkus.http.port}/user-info-endpoint
                                            quarkus.oidc.named-2.auth-server-url=${quarkus.oidc.auth-server-url}
                                            quarkus.oidc.named-2.tenant-paths=/user-info/named-tenant-2
                                            quarkus.oidc.named-2.discovery-enabled=false
                                            quarkus.oidc.named-2.jwks-path=protocol/openid-connect/certs
                                            quarkus.oidc.named-3.auth-server-url=${quarkus.oidc.auth-server-url}
                                            quarkus.oidc.named-3.tenant-paths=/user-info/named-tenant-3
                                            quarkus.oidc.named-3.discovery-enabled=false
                                            quarkus.oidc.named-3.jwks-path=protocol/openid-connect/certs
                                            quarkus.oidc.named-3.user-info-path=http://${quarkus.http.host}:${quarkus.http.port}/user-info-endpoint
                                            quarkus.oidc.named-3.authentication.user-info-required=false
                                            quarkus.http.auth.proactive=false
                                            """),
                            "application.properties"));

    @Test
    public void testDefaultTenant() {
        RestAssured.given().auth().oauth2(getAccessToken()).get("/user-info/default-tenant-random").then().statusCode(200)
                .body(Matchers.is("alice"));
    }

    @Test
    public void testNamedTenant() {
        RestAssured.given().auth().oauth2(getAccessToken()).get("/user-info/named-tenant-random").then().statusCode(200)
                .body(Matchers.is("alice"));
    }

    @Test
    public void testUserInfoNotRequiredWhenMissingUserInfoEndpoint() {
        RestAssured.given().auth().oauth2(getAccessToken()).get("/user-info/named-tenant-2").then().statusCode(200)
                .body(Matchers.is("false"));
    }

    @Test
    public void testUserInfoNotRequiredIfDisabledWhenUserInfoEndpointIsPresent() {
        RestAssured.given().auth().oauth2(getAccessToken()).get("/user-info/named-tenant-3").then().statusCode(200)
                .body(Matchers.is("false"));
    }

    private static String getAccessToken() {
        return new KeycloakTestClient().getAccessToken("alice", "alice", "quarkus-service-app", "secret", List.of("openid"));
    }

    public static class UserInfoEndpoint {
        void observe(@Observes Router router) {
            router.route("/user-info-endpoint").order(1).handler(rc -> rc.response().setStatusCode(200).end("{" +
                    "   \"sub\": \"123456789\"," +
                    "   \"preferred_username\": \"alice\"" +
                    "  }"));
        }
    }

    @Path("user-info")
    public static class UserInfoResource {

        @Inject
        UserInfo userInfo;

        @Inject
        TenantConfigBean tenantConfigBean;

        @Inject
        RoutingContext routingContext;

        @PermissionsAllowed("openid")
        @Path("default-tenant-random")
        @GET
        public String getDefaultTenantName() {
            if (!tenantConfigBean.getDefaultTenant().oidcConfig().authentication().userInfoRequired().orElse(false)) {
                throw new IllegalStateException("Default tenant user info should be required");
            }
            String tenantId = routingContext.get(OidcUtils.TENANT_ID_ATTRIBUTE);
            if (!OidcUtils.DEFAULT_TENANT_ID.equals(tenantId)) {
                throw new IllegalStateException(
                        "Incorrect tenant resolved based on the path - expected default tenant, got " + tenantId);
            }
            // assert tenant path added in the observer method
            assertTenantPathsContain("/extra-default-tenant-path");
            // assert tenant path added in the application.properties
            assertTenantPathsContain("/user-info/default-tenant-random");
            return userInfo.getPreferredUserName();
        }

        @PermissionsAllowed("openid")
        @Path("named-tenant-random")
        @GET
        public String getNamedTenantName() {
            if (!getNamedTenantConfig("named").authentication().userInfoRequired().orElse(false)) {
                throw new IllegalStateException("Named tenant user info should be required");
            }
            String tenantId = routingContext.get(OidcUtils.TENANT_ID_ATTRIBUTE);
            if (!"named".equals(tenantId)) {
                throw new IllegalStateException(
                        "Incorrect tenant resolved based on the path - expected 'named', got " + tenantId);
            }
            assertTenantPathsContain("/user-info/named-tenant-random");
            return userInfo.getPreferredUserName();
        }

        @PermissionsAllowed("openid")
        @Path("named-tenant-2")
        @GET
        public boolean getNamed2TenantUserInfoRequired() {
            return getNamedTenantConfig("named-2").authentication().userInfoRequired().orElse(false);
        }

        @PermissionsAllowed("openid")
        @Path("named-tenant-3")
        @GET
        public boolean getNamed3TenantUserInfoRequired() {
            return getNamedTenantConfig("named-3").authentication().userInfoRequired().orElse(false);
        }

        private OidcTenantConfig getNamedTenantConfig(String configName) {
            return tenantConfigBean.getStaticTenant(configName).oidcConfig();
        }

        private void assertTenantPathsContain(String tenantPath) {
            OidcTenantConfig tenantConfig = routingContext.get(OidcTenantConfig.class.getName());
            if (!tenantConfig.tenantPaths().get().contains(tenantPath)) {
                throw new IllegalStateException("Tenant config does not contain the tenant path " + tenantPath);
            }
        }
    }

    public static class OidcStartup {

        void observe(@Observes Oidc oidc, OidcConfig oidcConfig,
                @ConfigProperty(name = "quarkus.http.host") String host,
                @ConfigProperty(name = "quarkus.http.port") String port,
                @ConfigProperty(name = "quarkus.oidc.auth-server-url") String authServerUrl) {
            oidc.create(createDefaultTenant(oidcConfig));
            oidc.create(createNamedTenant(authServerUrl, host, port));
        }

        private static OidcTenantConfig createDefaultTenant(OidcConfig oidcConfig) {
            // this enhances 'application.properties' configuration with a tenant path
            return OidcTenantConfig.builder(OidcConfig.getDefaultTenant(oidcConfig))
                    .tenantPaths("/extra-default-tenant-path")
                    .build();
        }

        private static OidcTenantConfig createNamedTenant(String authServerUrl, String host, String port) {
            return OidcTenantConfig.authServerUrl(authServerUrl)
                    .tenantId("named")
                    .tenantPaths("/user-info/named-tenant-random")
                    .userInfoPath("http://%s:%s/user-info-endpoint".formatted(host, port))
                    .build();
        }
    }
}
