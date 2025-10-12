package io.quarkus.websockets.next.test.security;

import java.security.Principal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class SecurityIdentityPropagationWithRouteHandlerTest extends AbstractSecurityIdentityPropagationTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = getQuarkusUnitTest("quarkus.http.auth.propagate-security-identity=true",
            UserInfo.class, RouterObserver.class);

    @Test
    public void testSecurityIdentityAvailable() {
        // anonymous identity is allowed as no credentials
        RestAssured.given().get("/admin-user-info").then().statusCode(200).body(Matchers.is(""));
        // auth not required, but wrong credentials
        RestAssured.given()
                .auth().preemptive().basic("user", "wrong-credentials")
                .get("/admin-user-info").then().statusCode(401);

        // authenticated user with admin role
        RestAssured.given()
                .auth().preemptive().basic("admin", "admin")
                .get("/admin-user-info").then().statusCode(200).body(Matchers.is("admin"));
        // authenticated user without admin role
        RestAssured.given()
                .auth().preemptive().basic("martin", "martin")
                .get("/admin-user-info").then().statusCode(200).body(Matchers.is(""));
    }

    @Test
    public void testPrincipalAvailable() {
        RestAssured.given().get("/user-info-principal").then().statusCode(200).body(Matchers.is(""));
        RestAssured.given()
                .auth().preemptive().basic("admin", "admin")
                .get("/user-info-principal").then().statusCode(200).body(Matchers.is("admin"));
        RestAssured.given()
                .auth().preemptive().basic("martin", "martin")
                .get("/user-info-principal").then().statusCode(200).body(Matchers.is("martin"));
    }

    public static class RouterObserver {

        public void route(@Observes Router router, UserInfo userInfo) {
            router.route("/admin-user-info").handler(event -> event.response().end(userInfo.getAdminPrincipalName()));
            router.route("/user-info-principal").handler(event -> event.response().end(userInfo.getPrincipalName()));
        }
    }

    @ApplicationScoped
    public static class UserInfo {

        @Inject
        SecurityIdentity identity;

        @Inject
        Principal principal;

        @ActivateRequestContext
        String getAdminPrincipalName() {
            if (identity.hasRole("admin")) {
                return identity.getPrincipal().getName();
            }
            return "";
        }

        @ActivateRequestContext
        String getPrincipalName() {
            return principal.getName();
        }

    }
}
