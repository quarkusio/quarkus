package io.quarkus.vertx.http.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = Format.PKCS12, client = true))
public class FluentApiMTLSAuthenticationRequiredDevModeTest {

    private static final String HOT_RELOAD_FILTER = "HotReloadFilter.java";
    private static final String AAAA = "AAAA";
    private static final String BBBB = "BBBB";

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(AuthMechanismConfig.class, PathHandler.class, HotReloadFilter.class)
            .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "server-keystore.p12")
            .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12"));

    @TestHTTPResource(value = "/mtls", tls = true)
    URL adminRoleTlsPath;

    @TestHTTPResource(value = "/public", tls = true)
    URL publicTlsPath;

    @TestHTTPResource(value = "/public")
    URL publicPath;

    @RepeatedTest(2) // test reloading, this is important so that we verify this API works in DEV mode
    void testCustomCertificateToRolesMapper(RepetitionInfo info) {
        URL url = correctPortIfNecessary(adminRoleTlsPath);
        final String headerValue = modifyFileAndGetHeaderValue(info);
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> RestAssured.given()
                .keyStore("target/certs/mtls-test-client-keystore.p12", "secret")
                .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                .get(url).then()
                .statusCode(200)
                .header("X-Header", headerValue)
                .body(is("CN=localhost:/mtls")));
        // expect failure for path that requires 'admin' role
        try {
            RestAssured.given()
                    .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                    .get(url);
            Assertions.fail("SSL handshake failure not detected");
        } catch (Exception e) {
            assertThat(e.getMessage())
                    .containsAnyOf("Received fatal alert: bad_certificate", "Received fatal alert: certificate_required")
                    .describedAs("Expected SSL handshake failure, but got: " + e);
        }
    }

    @Test
    void testPublicPathRequiresClientAuth() {
        URL url = correctPortIfNecessary(publicTlsPath);
        // expect failure for public path because the client authentication is required
        try {
            RestAssured.given()
                    .trustStore("target/certs/mtls-test-client-truststore.p12", "secret")
                    .get(url);
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
            URL url = correctPortIfNecessary(publicPath);
            RestAssured.given().get(url);
            Assertions.fail("Connection should be refused");
        } catch (Exception e) {
            var sslHandshakeFailed = e.getMessage().contains("Connection refused");
            assertTrue(sslHandshakeFailed, () -> "Expected the connection to be refused, but got: " + e.getMessage());
        }
    }

    private static String modifyFileAndGetHeaderValue(RepetitionInfo info) {
        final String headerValue;
        if (info.getCurrentRepetition() == 1) {
            headerValue = AAAA;
        } else {
            headerValue = BBBB;
            test.modifySourceFile(HOT_RELOAD_FILTER, s -> s.replace(AAAA, BBBB));
        }
        return headerValue;
    }

    private static URL correctPortIfNecessary(URL url) {
        // DEV mode is started with 8443 but the injected URL has 8444, so correct it until this is fixed
        // I don't want to set SSL port explicitly as want to test config when no application properties are set
        if (url.getPort() == 8444) {
            try {
                return new URL(url.getProtocol(), url.getHost(), 8443, url.getFile());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return url;
    }
}
