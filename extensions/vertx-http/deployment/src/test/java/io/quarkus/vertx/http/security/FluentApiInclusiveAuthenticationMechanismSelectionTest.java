package io.quarkus.vertx.http.security;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.net.URL;
import java.util.Set;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.ClientAuth;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = Format.PKCS12, client = true))
class FluentApiInclusiveAuthenticationMechanismSelectionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(AuthMechanismConfig.class, TestIdentityController.class, TestIdentityProvider.class,
                    PathHandler.class, TestTrustedIdentityProvider.class, CustomHttpSecurityPolicy.class,
                    CustomSchemeAuthenticationMechanism.class, AbstractCustomAuthenticationMechanism.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.form.enabled=true
                    quarkus.http.ssl.certificate.key-store-file=server-keystore.p12
                    quarkus.http.ssl.certificate.key-store-password=secret
                    quarkus.http.ssl.certificate.trust-store-file=server-truststore.p12
                    quarkus.http.ssl.certificate.trust-store-password=secret
                    quarkus.http.auth.form.login-page=
                    quarkus.http.auth.inclusive=true
                    """), "application.properties")
            .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
            .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12"));

    @TestHTTPResource(value = "/mtls-basic-inclusive", tls = true)
    URL mTlsBasicInclusiveUrl;

    @TestHTTPResource(value = "/mtls-basic-form-inclusive", tls = true)
    URL mTlsBasicFormInclusiveUrl;

    @BeforeAll
    static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", new StringPermission("openid"), new StringPermission("email"),
                        new StringPermission("profile"));
    }

    @Test
    void testCombiningBasicAndMutualTls_inclusiveAuthentication() {
        // anonymous user
        RestAssured.given().get("/mtls-basic-inclusive").then().statusCode(401);

        // other mechanism, that must not be allowed for this path
        RestAssured.given().header("custom-auth", "ignored").get("/mtls-basic-inclusive").then().statusCode(401);

        // only basic auth - fail as we require both basic and mTLS
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .when()
                .get("/mtls-basic-inclusive")
                .then()
                .statusCode(401);

        // only mTLS - fail as we require both basic and mTLS
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(mTlsBasicInclusiveUrl).then().statusCode(401);

        // both basic and mTLS auth
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .auth().preemptive().basic("admin", "admin")
                .get(mTlsBasicInclusiveUrl).then().statusCode(200).body(is("CN=localhost:/mtls-basic-inclusive"));
    }

    @Test
    void testCombiningBasicAndMutualTlsAndForm_inclusiveAuthentication() {
        // anonymous user
        RestAssured.given().redirects().follow(false).get("/mtls-basic-form-inclusive").then().statusCode(401);

        // other mechanism, that must not be allowed for this path
        RestAssured.given().redirects().follow(false).header("custom-auth", "ignored").get("/mtls-basic-form-inclusive").then()
                .statusCode(401);

        // only basic auth - fail as we require all three - basic, form and mTLS
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .when()
                .get("/mtls-basic-form-inclusive")
                .then()
                .statusCode(401);

        // only mTLS - fail as we require all three - basic, form and mTLS
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(mTlsBasicFormInclusiveUrl)
                .then()
                .statusCode(401);

        // only form - fail as we require all three - basic, form and mTLS
        CookieFilter adminCookies = new CookieFilter();
        loginUsingFormAuth(adminCookies, "admin");
        RestAssured
                .given()
                .filter(adminCookies)
                .get("/mtls-basic-form-inclusive")
                .then()
                .statusCode(401);

        // all three - basic, form and mTLS auth
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .filter(adminCookies)
                .auth().preemptive().basic("admin", "admin")
                .get(mTlsBasicFormInclusiveUrl)
                .then()
                .statusCode(200)
                .body(is("CN=localhost:/mtls-basic-form-inclusive"));
    }

    private static void loginUsingFormAuth(CookieFilter adminCookies, String user) {
        RestAssured
                .given()
                .filter(adminCookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", user)
                .formParam("j_password", user)
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/index.html"))
                .cookie("quarkus-credential",
                        detailedCookie().value(notNullValue()).path(equalTo("/")));
    }

    static class AuthMechanismConfig {

        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity
                    .mechanism(new CustomSchemeAuthenticationMechanism())
                    .basic()
                    .mTLS(MTLS.builder().authentication(ClientAuth.REQUEST).rolesMapping("localhost", "admin").build())
                    .path("/mtls-basic-inclusive").authenticatedWith(Set.of("basic", "x509")).roles("admin")
                    .path("/mtls-basic-form-inclusive").authenticatedWith(Set.of("form", "basic", "x509")).roles("admin");
        }

    }

    static final class CustomHttpSecurityPolicy implements HttpSecurityPolicy {
        @Override
        public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identityUni,
                AuthorizationRequestContext requestContext) {
            return identityUni.onItemOrFailure().transform((identity, throwable) -> {
                if (throwable == null && identity.hasRole("admin")) {
                    return CheckResult.PERMIT;
                }
                return CheckResult.DENY;
            });
        }
    }

    static class CustomSchemeAuthenticationMechanism extends AbstractCustomAuthenticationMechanism {
        @Override
        public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
            return Uni.createFrom()
                    .item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "custom-scheme"));
        }
    }

    static abstract class AbstractCustomAuthenticationMechanism implements HttpAuthenticationMechanism {
        private final HttpAuthenticationMechanism delegate = new BasicAuthenticationMechanism(null, false);

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            if (context.request().headers().get("custom-auth") != null) {
                return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                        .setPrincipal(new QuarkusPrincipal("Olga"))
                        .addRole("admin")
                        .build());
            }
            return delegate.authenticate(context, identityProviderManager);
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return delegate.getChallenge(context);
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return delegate.getCredentialTypes();
        }
    }
}
