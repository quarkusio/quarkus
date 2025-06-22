package io.quarkus.vertx.http.security;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.net.URL;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
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
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = Format.PKCS12, client = true))
public class FluentApiAuthenticationMechanismSelectionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(AuthMechanismConfig.class, TestIdentityController.class, TestIdentityProvider.class,
                    PathHandler.class, TestTrustedIdentityProvider.class, CustomHttpSecurityPolicy.class,
                    CustomSchemeAuthenticationMechanism.class, AbstractCustomAuthenticationMechanism.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.form.enabled=true
                    quarkus.http.auth.basic=true
                    quarkus.http.ssl.client-auth=request
                    quarkus.http.ssl.certificate.key-store-file=server-keystore.p12
                    quarkus.http.ssl.certificate.key-store-password=secret
                    quarkus.http.ssl.certificate.trust-store-file=server-truststore.p12
                    quarkus.http.ssl.certificate.trust-store-password=secret
                    """), "application.properties")
            .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
            .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12"));

    @TestHTTPResource(value = "/mtls", tls = true)
    URL url;

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", new StringPermission("openid"), new StringPermission("email"),
                        new StringPermission("profile"))
                .add("user", "user", new StringPermission("profile"));
    }

    @Test
    public void testForm() {
        CookieFilter adminCookies = new CookieFilter();
        loginUsingFormAuth(adminCookies, "admin");

        // valid request
        RestAssured
                .given()
                .filter(adminCookies)
                .redirects().follow(false)
                .when()
                .get("/form/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:/form/admin"));

        // the same valid request but policy is applied because we added the header, so expect a failure
        CookieFilter userCookies = new CookieFilter();
        loginUsingFormAuth(userCookies, "user");
        RestAssured
                .given()
                .filter(userCookies)
                .redirects().follow(false)
                .header("fail", "ignored")
                .when()
                .get("/form/admin")
                .then()
                .assertThat()
                .statusCode(403);

        // basic authentication -> authentication must fail
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .get("/form/admin")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login.html"));

        // basic authentication & POST -> access is going to be denied as there are permissions with POST method
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .post("/form/admin")
                .then()
                .assertThat()
                .statusCode(403);

        // basic authentication & PUT -> access is granted because this method is specifically configured for 'basic'
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .redirects().follow(false)
                .when()
                .put("/form/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:/form/admin"));
    }

    @Test
    public void testBasicAuth() {
        basicAuthTest("/basic/admin", "admin:/basic/admin");
    }

    @Test
    public void testCustomAuthenticationMechanismScheme() {
        basicAuthTest("/custom-scheme/admin", "admin:/custom-scheme/admin");
    }

    @Test
    public void testCustomAuthenticationMechanismInstance() {
        basicAuthTest("/custom-instance/admin", "admin:/custom-instance/admin");
    }

    @Test
    public void testMutualTlsMechanism() {
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(url).then().statusCode(200).body(is("CN=localhost:/mtls"));
        RestAssured.given()
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(url).then().statusCode(401);
        try {
            RestAssured
                    .given()
                    .auth().preemptive().basic("admin", "admin")
                    .when()
                    .get(url)
                    .then()
                    .assertThat()
                    .statusCode(401);
            Assertions.fail("Request should had fail because mTLS must be required");
        } catch (Exception exception) {
            Assertions.assertTrue(exception.getMessage().contains("PKIX path building failed"));
        }
    }

    private static void basicAuthTest(String s, String operand) {
        RestAssured
                .given()
                .auth().preemptive().basic("admin", "admin")
                .when()
                .get(s)
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo(operand));
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .when()
                .get(s)
                .then()
                .assertThat()
                .statusCode(403);
        RestAssured
                .given()
                .get(s)
                .then()
                .assertThat()
                .statusCode(401);
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

    public static class AuthMechanismConfig {

        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity
                    .get("/form/admin").form().authorization()
                    .policy(identity -> "admin".equals(identity.getPrincipal().getName()))
                    .put("/form/admin").basic().authorization()
                    .policy(identity -> "admin".equals(identity.getPrincipal().getName()))
                    .path("/basic/admin").methods("GET").basic().authorization().permissions("openid", "email", "profile")
                    .path("/custom-scheme/admin").authenticatedWith("custom-scheme").policy(new CustomHttpSecurityPolicy())
                    .path("/custom-instance/admin").authenticatedWith(new AbstractCustomAuthenticationMechanism() {
                    }).authorization().policy((identity, event) -> identity.hasRole("admin")
                            && event.normalizedPath().endsWith("/custom-instance/admin"))
                    .path("/mtls").mTLS();
        }

    }

    public static final class CustomHttpSecurityPolicy implements HttpSecurityPolicy {
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

    @ApplicationScoped
    public static class CustomSchemeAuthenticationMechanism extends AbstractCustomAuthenticationMechanism {
        @Override
        public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
            return Uni.createFrom()
                    .item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "custom-scheme"));
        }
    }

    public static abstract class AbstractCustomAuthenticationMechanism implements HttpAuthenticationMechanism {
        private final HttpAuthenticationMechanism delegate = new BasicAuthenticationMechanism(null);

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
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
