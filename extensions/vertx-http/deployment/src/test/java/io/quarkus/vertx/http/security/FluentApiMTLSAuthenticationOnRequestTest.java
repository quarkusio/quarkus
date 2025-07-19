package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.net.URL;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.http.ClientAuth;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = Format.PKCS12, client = true))
public class FluentApiMTLSAuthenticationOnRequestTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(AuthMechanismConfig.class, PathHandler.class)
            .addAsResource(new StringAsset("""
                    quarkus.tls.cert-1.key-store.p12.path=server-keystore.p12
                    quarkus.tls.cert-1.key-store.p12.password=secret
                    quarkus.tls.cert-1.trust-store.p12.path=server-truststore.p12
                    quarkus.tls.cert-1.trust-store.p12.password=secret
                    quarkus.http.tls-configuration-name=cert-1
                    """), "application.properties")
            .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
            .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12"));

    @TestHTTPResource(value = "/mtls", tls = true)
    URL url;

    @TestHTTPResource(value = "/public", tls = true)
    URL publicTlsPath;

    @TestHTTPResource(value = "/public")
    URL publicPath;

    @Test
    public void testClientAuthEnforcedWhenAuthRequired() {
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(url).then().statusCode(200).body(is("CN=localhost:/mtls"));
        RestAssured.given()
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(url).then().statusCode(401);
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

    public static class AuthMechanismConfig {

        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity
                    .mTLS(MTLS.builder().authentication(ClientAuth.REQUEST).rolesMapping("localhost", "admin").build())
                    .path("/public").permit()
                    .path("/mtls").roles("admin");
        }

    }

}
