package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

class CombinedFormBasicAuthGlobalInclusiveTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot(jar -> jar
            .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                    PathHandler.class, CustomMechanism.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.inclusive=true
                    quarkus.http.auth.basic=true
                    quarkus.http.auth.realm=TestRealm
                    quarkus.http.auth.form.enabled=true
                    quarkus.http.auth.form.login-page=
                    quarkus.http.auth.form.error-page=
                    quarkus.http.auth.form.landing-page=
                    quarkus.http.auth.policy.r1.roles-allowed=admin
                    quarkus.http.auth.permission.roles1.paths=/admin
                    quarkus.http.auth.permission.roles1.policy=r1
                    quarkus.http.auth.permission.roles1.auth-mechanism=form,basic
                    """), "application.properties"));

    @BeforeAll
    static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    void testOtherMechanismNotAllowed() {
        // tests that:
        // - this path requires form and basic
        // - inclusive auth selected form and basic, so the custom is not required for /admin path
        RestAssured
                .given()
                .header("use-custom-auth", "true")
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .statusCode(401);
    }

    @Test
    void testFormBasedAuthOnlyFails() {
        CookieFilter cookies = new CookieFilter();

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .statusCode(200);

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .statusCode(401);
    }

    @Test
    void testBasicAuthOnlyFails() {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .get("/admin")
                .then()
                .statusCode(401);
    }

    @Test
    public void testBasicAndFormAuthTogetherSucceeds() {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .statusCode(200);

        RestAssured
                .given()
                .filter(cookies)
                .auth().preemptive().basic("admin", "admin")
                .when()
                .get("/admin")
                .then()
                .statusCode(200)
                .body(equalTo("admin:/admin"));
    }

    @ApplicationScoped
    static class CustomMechanism implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            if (context.request().headers().contains("use-custom-auth")) {
                return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                        .addRole("admin")
                        .setPrincipal(new QuarkusPrincipal("custom"))
                        .build());
            }
            return Uni.createFrom().nullItem();
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return Uni.createFrom().nullItem();
        }

        @Override
        public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
            return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.POST, "ignored", "custom"));
        }
    }
}
