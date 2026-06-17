package io.quarkus.vertx.http.proxy;

import static io.quarkus.vertx.http.proxy.AbstractTrustedProxyDnTest.PASSWORD;

import java.io.File;
import java.net.URL;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.quarkus.vertx.http.runtime.ForwardedServerRequestWrapper;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.http.HttpServerRequest;

/**
 * Verifies that request with the "Forwarded" header a from trusted proxy indicating "proto=https"
 * can use the HTTP protocol. The {@link io.quarkus.vertx.http.runtime.security.MtlsAuthenticationMechanism}
 * must not rely on the {@link HttpServerRequest#isSSL()} as TLS clients certificates may not be available.
 *
 * @see ForwardedServerRequestWrapper#isSSL() for reasons why it can indicate SSL/TLS communicate
 *      while {@link ForwardedServerRequestWrapper#sslSession()} is null
 */
@Certificates(baseDir = "target/certs", replaceIfExists = true, certificates = @Certificate(name = ForwardedProtoHttpsOnHttpConnectionTest.CERT_NAME, password = PASSWORD, formats = {
        Format.PKCS12 }, client = true))
class ForwardedProtoHttpsOnHttpConnectionTest {

    static final String CERT_NAME = "forwarded-proto-https-on-http";
    private static final String CERTS_DIR = "target/certs/";

    private static final String configuration = """
            quarkus.tls.key-store.p12.path=%1$s-keystore.p12
            quarkus.tls.key-store.p12.password=%2$s
            quarkus.tls.trust-store.p12.path=%1$s-server-truststore.p12
            quarkus.tls.trust-store.p12.password=%2$s
            quarkus.http.ssl.client-auth=REQUEST
            quarkus.http.proxy.proxy-address-forwarding=true
            quarkus.http.proxy.allow-forwarded=true
            """.formatted(CERTS_DIR + CERT_NAME, PASSWORD);

    @TestHTTPResource(value = "/forward")
    URL httpUrl;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File(CERTS_DIR + CERT_NAME + "-keystore.p12"), "server-keystore.p12")
                    .addAsResource(new File(CERTS_DIR + CERT_NAME + "-server-truststore.p12"),
                            "server-truststore.p12"));

    @Test
    void httpRequestWithForwardedProtoHttpsShouldNotCauseNPE() {
        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .get(httpUrl)
                .then()
                .statusCode(200);
    }
}
