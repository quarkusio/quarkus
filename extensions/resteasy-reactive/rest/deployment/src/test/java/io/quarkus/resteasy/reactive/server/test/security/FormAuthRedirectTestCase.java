package io.quarkus.resteasy.reactive.server.test.security;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.mutiny.Uni;

public class FormAuthRedirectTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, FormAuthResource.class,
                            TrustedIdentityProvider.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.form.enabled=true
                            quarkus.http.auth.form.landing-page=/hello
                            quarkus.http.auth.form.new-cookie-interval=PT1S
                            """), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("a d m i n", "a d m i n", "a d m i n")
                .add("user", "user");
    }

    @Test
    public void testFormAuthFailure() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured
                .given()
                .filter(new CookieFilter())
                .redirects().follow(false)
                .when()
                .formParam("j_username", "a d m i n")
                .formParam("j_password", "wrongpassword")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/error"))
                .header("quarkus-credential", nullValue());
    }

    @Test
    public void testFormAuthLoginLogout() throws InterruptedException {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        CookieFilter cookies = new CookieFilter();
        var response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/hello")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login.html"))
                .extract();
        assertNull(response.cookie("quarkus-credential"));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "user")
                .formParam("j_password", "user")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/hello"))
                .cookie("quarkus-credential", detailedCookie().value(notNullValue()).sameSite("Strict").path("/"));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/hello")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("hello user"));

        Thread.sleep(Duration.ofSeconds(2).toMillis());

        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/logout")
                .then()
                .assertThat()
                .statusCode(303)
                .header("location", containsString("/"))
                .extract();
        String credentialsCookieValue = response.cookie("quarkus-credential");
        Assertions.assertTrue(credentialsCookieValue == null || credentialsCookieValue.isEmpty(),
                "Expected credentials cookie was removed, but actual value was " + credentialsCookieValue);

        response = RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/hello")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login.html"))
                .extract();
        credentialsCookieValue = response.cookie("quarkus-credential");
        Assertions.assertTrue(credentialsCookieValue == null || credentialsCookieValue.isEmpty());
    }

    @Path("/")
    public static class FormAuthResource {

        private final CurrentIdentityAssociation identity;

        public FormAuthResource(CurrentIdentityAssociation identity) {
            this.identity = identity;
        }

        @Authenticated
        @GET
        @Path("hello")
        public String hello() {
            return "hello " + identity.getIdentity().getPrincipal().getName();
        }

        @GET
        @Path("logout")
        public Response logout() {
            if (identity.getIdentity().isAnonymous()) {
                throw new UnauthorizedException("Not authenticated");
            }
            FormAuthenticationMechanism.logout(identity.getIdentity());
            return Response.seeOther(URI.create("/"))
                    .build();
        }
    }

    @ApplicationScoped
    public static class TrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {
        @Override
        public Class<TrustedAuthenticationRequest> getRequestType() {
            return TrustedAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest trustedAuthenticationRequest,
                AuthenticationRequestContext authenticationRequestContext) {
            if ("user".equals(trustedAuthenticationRequest.getPrincipal())) {
                return Uni.createFrom()
                        .item(QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal("user")).build());
            }
            return Uni.createFrom().nullItem();
        }
    }
}
