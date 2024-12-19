package io.quarkus.oidc.test;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class BearerAuthenticationLightweightDevServicesTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.oidc.devservices.lightweight.enabled=true
                            """), "application.properties"));

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String serverUrl;

    @Test
    public void testLoginAsCustomUser() {
        RestAssured.given()
                .auth().oauth2(getAccessToken("Ronald", "admin"))
                .get("/secured/admin-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Ronald"))
                .body(Matchers.containsString("admin"));
        RestAssured.given()
                .auth().oauth2(getAccessToken("Ronald", "admin"))
                .get("/secured/user-only")
                .then()
                .statusCode(403);
    }

    @Test
    public void testLoginAsAlice() {
        RestAssured.given()
                .auth().oauth2(getAccessToken("alice"))
                .get("/secured/admin-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("alice"))
                .body(Matchers.containsString("admin"))
                .body(Matchers.containsString("user"));
        RestAssured.given()
                .auth().oauth2(getAccessToken("alice"))
                .get("/secured/user-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("alice"))
                .body(Matchers.containsString("admin"))
                .body(Matchers.containsString("user"));
    }

    @Test
    public void testLoginAsBob() {
        RestAssured.given()
                .auth().oauth2(getAccessToken("bob"))
                .get("/secured/admin-only")
                .then()
                .statusCode(403);
        RestAssured.given()
                .auth().oauth2(getAccessToken("bob"))
                .get("/secured/user-only")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("bob"))
                .body(Matchers.containsString("user"));
    }

    private String getAccessToken(String user) {
        return RestAssured.given().get(serverUrl + "/testing/generate/access-token?user=" + user).asString();
    }

    private String getAccessToken(String user, String... roles) {
        return RestAssured.given()
                .get(serverUrl + "/testing/generate/access-token?user=" + user + "&roles=" + String.join(",", roles))
                .asString();
    }

    @Path("secured")
    public static class SecuredResource {

        @Inject
        SecurityIdentity securityIdentity;

        @Inject
        UserInfo userInfo;

        @RolesAllowed("admin")
        @GET
        @Path("admin-only")
        public String getAdminOnly() {
            return securityIdentity.getPrincipal().getName() + " " + securityIdentity.getRoles();
        }

        @RolesAllowed("user")
        @GET
        @Path("user-only")
        public String getUserOnly() {
            return userInfo.getPreferredUserName() + " " + securityIdentity.getRoles();
        }

    }

}
