package io.quarkus.oidc.test;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class UserInfoRequiredDetectionTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UserInfoResource.class, UserInfoEndpoint.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.oidc.tenant-paths=/user-info/default-tenant
                                            quarkus.oidc.user-info-path=http://${quarkus.http.host}:${quarkus.http.port}/user-info-endpoint
                                            quarkus.oidc.named.auth-server-url=${quarkus.oidc.auth-server-url}
                                            quarkus.oidc.named.tenant-paths=/user-info/named-tenant
                                            quarkus.oidc.named.user-info-path=http://${quarkus.http.host}:${quarkus.http.port}/user-info-endpoint
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
        RestAssured.given().auth().oauth2(getAccessToken()).get("/user-info/default-tenant").then().statusCode(200)
                .body(Matchers.is("alice"));
    }

    @Test
    public void testNamedTenant() {
        RestAssured.given().auth().oauth2(getAccessToken()).get("/user-info/named-tenant").then().statusCode(200)
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
        OidcConfig config;

        @Inject
        UserInfo userInfo;

        @PermissionsAllowed("openid")
        @Path("default-tenant")
        @GET
        public String getDefaultTenantName() {
            if (!config.defaultTenant.authentication.userInfoRequired.orElse(false)) {
                throw new IllegalStateException("Default tenant user info should be required");
            }
            return userInfo.getPreferredUserName();
        }

        @PermissionsAllowed("openid")
        @Path("named-tenant")
        @GET
        public String getNamedTenantName() {
            if (!config.namedTenants.get("named").authentication.userInfoRequired.orElse(false)) {
                throw new IllegalStateException("Named tenant user info should be required");
            }
            return userInfo.getPreferredUserName();
        }

        @PermissionsAllowed("openid")
        @Path("named-tenant-2")
        @GET
        public boolean getNamed2TenantUserInfoRequired() {
            return config.namedTenants.get("named-2").authentication.userInfoRequired.orElse(false);
        }

        @PermissionsAllowed("openid")
        @Path("named-tenant-3")
        @GET
        public boolean getNamed3TenantUserInfoRequired() {
            return config.namedTenants.get("named-3").authentication.userInfoRequired.orElse(false);
        }
    }

}
