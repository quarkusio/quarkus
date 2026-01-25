package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.net.URL;

import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.http.ClientAuth;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = Format.PKCS12, client = true))
class FluentApiMechanismPrioritizationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot(jar -> jar
            .addClasses(AuthMechanismConfig.class, PathHandler.class)
            .addAsResource(new StringAsset("""
                    quarkus.tls.cert-1.key-store.p12.path=server-keystore.p12
                    quarkus.tls.cert-1.key-store.p12.password=secret
                    quarkus.tls.cert-1.trust-store.p12.path=server-truststore.p12
                    quarkus.tls.cert-1.trust-store.p12.password=secret
                    quarkus.http.tls-configuration-name=cert-1
                    """), "application.properties")
            .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
            .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12")
            .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class));

    @TestHTTPResource(value = "/admin", tls = true)
    URL adminUrl;

    @TestHTTPResource(value = "/public", tls = true)
    URL publicTlsPath;

    @TestHTTPResource(value = "/public")
    URL publicPath;

    @BeforeAll
    static void setup() {
        TestIdentityController.resetRoles().add("Stuart", "Stuart", "admin");
    }

    @Test
    void testClientAuthEnforcedWhenAuthRequired() {
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(adminUrl).then().statusCode(200).body(is("CN=localhost:/admin"));
        RestAssured.given()
                .redirects().follow(false)
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(adminUrl).then()
                .header("Location", Matchers.containsString("/login.html"))
                .statusCode(302);
    }

    @Test
    void testPublicPathDoesNotRequireClientAuth() {
        RestAssured.given()
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(publicTlsPath)
                .then().statusCode(200).body(is(":/public"));
    }

    @Test
    void testInsecureRequestsAllowed() {
        RestAssured.given()
                .get(publicPath)
                .then().statusCode(200).body(is(":/public"));
    }

    @Test
    void testMutualTlsHasHigherPriorityThanBasicAuth() {
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .auth().preemptive().basic("Stuart", "Stuart")
                .get(adminUrl).then().statusCode(200).body(is("CN=localhost:/admin"));
    }

    @Test
    void testFormHigherPriorityThanMutualTls() {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "Stuart")
                .formParam("j_password", "Stuart")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(200);

        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .filter(cookies)
                .get(adminUrl)
                .then()
                .statusCode(200)
                .body(is("Stuart:/admin"));
    }

    static class AuthMechanismConfig {

        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity
                    .mTLS(MTLS.builder().authentication(ClientAuth.REQUEST).rolesMapping("localhost", "admin").priority(2)
                            .build())
                    .mechanism(Basic.priority(1))
                    .mechanism(Form.builder().priority(3).landingPage(null).build())
                    .path("/public").permit()
                    .path("/admin").roles("admin");
        }

    }

}
