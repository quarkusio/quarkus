package io.quarkus.vertx.http.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.tls.BaseTlsConfiguration;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = Format.PKCS12, client = true))
public class FluentApiMTLSAuthenticationRequiredTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(AuthMechanismConfig.class, PathHandler.class, MyTlsConfiguration.class)
            .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
            .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12"));

    @TestHTTPResource(value = "/mtls", tls = true)
    URL adminRoleTlsPath;

    @TestHTTPResource(value = "/public", tls = true)
    URL publicTlsPath;

    @TestHTTPResource(value = "/public")
    URL publicPath;

    @Test
    void testCustomCertificateToRolesMapper() {
        RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(adminRoleTlsPath).then().statusCode(200).body(is("CN=localhost:/mtls"));
        // expect failure for path that requires 'admin' role
        try {
            RestAssured.given()
                    .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                    .get(adminRoleTlsPath);
            Assertions.fail("SSL handshake failure not detected");
        } catch (Exception e) {
            assertThat(e.getMessage())
                    .containsAnyOf("Received fatal alert: bad_certificate", "Received fatal alert: certificate_required")
                    .describedAs("Expected SSL handshake failure, but got: " + e);
        }
    }

    @Test
    void testPublicPathRequiresClientAuth() {
        // expect failure for public path because the client authentication is required
        try {
            RestAssured.given()
                    .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                    .get(publicTlsPath);
            Assertions.fail("SSL handshake failure not detected");
        } catch (Exception e) {
            assertThat(e.getMessage())
                    .containsAnyOf("Received fatal alert: bad_certificate", "Received fatal alert: certificate_required")
                    .describedAs("Expected SSL handshake failure, but got: " + e);
        }
    }

    @Test
    void testInsecureRequestsDenied() {
        try {
            RestAssured.given().get(publicPath);
            Assertions.fail("Connection should be refused");
        } catch (Exception e) {
            var sslHandshakeFailed = e.getMessage().contains("Connection refused");
            assertTrue(sslHandshakeFailed, () -> "Expected the connection to be refused, but got: " + e.getMessage());
        }
    }

    public static class AuthMechanismConfig {

        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity
                    .mTLS(
                            MTLS.builder()
                                    .tls("cert-1", new MyTlsConfiguration())
                                    .certificateToRolesMapper(x509Certificate -> "CN=localhost".equals(
                                            x509Certificate.getIssuerX500Principal().getName()) ? Set.of("admin") : Set.of())
                                    .build())
                    .path("/public").permit()
                    .path("/mtls").roles("admin");
        }

    }

    public static class MyTlsConfiguration extends BaseTlsConfiguration {
        @Override
        public KeyCertOptions getKeyStoreOptions() {
            return new KeyStoreOptions()
                    .setPath("target/certs/mtls-test-keystore.p12")
                    .setPassword("secret")
                    .setType("PKCS12");
        }

        @Override
        public TrustOptions getTrustStoreOptions() {
            return new KeyStoreOptions()
                    .setPath("target/certs/mtls-test-server-truststore.p12")
                    .setPassword("secret")
                    .setType("PKCS12");
        }

        @Override
        public SSLOptions getSSLOptions() {
            SSLOptions options = new SSLOptions();
            options.setKeyCertOptions(getKeyStoreOptions());
            options.setTrustOptions(getTrustStoreOptions());
            options.setSslHandshakeTimeoutUnit(TimeUnit.SECONDS);
            options.setSslHandshakeTimeout(10);
            options.setEnabledSecureTransportProtocols(Set.of("TLSv1.3", "TLSv1.2"));
            return options;
        }
    }
}
