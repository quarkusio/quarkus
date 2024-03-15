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
                                            quarkus.oidc.named.authentication.user-info-required=true
                                            quarkus.oidc.named.user-info-path=http://${quarkus.http.host}:${quarkus.http.port}/user-info-endpoint
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
    }

}
