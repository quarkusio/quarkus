package io.quarkus.vertx.http.security;

import java.security.Principal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.Router;

public class SecurityIdentityPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RouterObserver.class, UserInfo.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.propagate-security-identity=true"),
                            "application.properties"));

    @Test
    public void testSecurityIdentityAvailable() {
        // anonymous identity is allowed as no credentials
        RestAssured.given().get("/admin-user-info").then().statusCode(200).body(Matchers.is(""));
        // auth not required, but wrong credentials
        RestAssured.given()
                .auth().preemptive().basic("user", "user")
                .get("/admin-user-info").then().statusCode(401);

        // authenticated user with admin role
        RestAssured.given()
                .auth().preemptive().basic("admin", "admin")
                .get("/admin-user-info").then().statusCode(200).body(Matchers.is("admin"));
        // authenticated user without admin role
        RestAssured.given()
                .auth().preemptive().basic("harvey", "harvey")
                .get("/admin-user-info").then().statusCode(200).body(Matchers.is(""));
    }

    @Test
    public void testPrincipalAvailable() {
        RestAssured.given().get("/user-info-principal").then().statusCode(200).body(Matchers.is(""));
        RestAssured.given()
                .auth().preemptive().basic("admin", "admin")
                .get("/user-info-principal").then().statusCode(200).body(Matchers.is("admin"));
        RestAssured.given()
                .auth().preemptive().basic("harvey", "harvey")
                .get("/user-info-principal").then().statusCode(200).body(Matchers.is("harvey"));
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

    @ApplicationScoped
    public static class BasicAuthProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

        @Override
        public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
            return UsernamePasswordAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
                AuthenticationRequestContext authenticationRequestContext) {
            String username = request.getUsername();
            boolean isAdmin = "admin".equalsIgnoreCase(username);
            if (isAdmin || "harvey".equalsIgnoreCase(username)) {
                var identity = QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal(username))
                        .setAnonymous(false)
                        .addRole(isAdmin ? "admin" : "harvey")
                        .build();
                return Uni.createFrom().item(identity);
            }
            return Uni.createFrom().nullItem();
        }
    }
}
