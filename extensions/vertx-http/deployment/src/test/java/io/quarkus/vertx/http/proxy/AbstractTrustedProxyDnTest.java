package io.quarkus.vertx.http.proxy;

import static io.quarkus.vertx.http.proxy.AbstractTrustedProxyDnTest.PASSWORD;

import java.io.File;
import java.net.URL;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(certificates = @Certificate(name = AbstractTrustedProxyDnTest.CERT_NAME, password = PASSWORD, formats = {
        Format.PKCS12 }, client = true), replaceIfExists = true, baseDir = "target/certs")
abstract class AbstractTrustedProxyDnTest {

    static final String CERT_NAME = "proxy-dn-test";
    static final String PASSWORD = "secret";
    static final String CERTS_DIR = "target/certs/";

    @TestHTTPResource(value = "/trusted-proxy", tls = true)
    URL tlsUrl;

    static QuarkusExtensionTest createTest(String clientAuth, String... trustedProxyDns) {
        final var sb = new StringBuilder("""
                quarkus.tls.key-store.p12.path=%1$s-keystore.p12
                quarkus.tls.key-store.p12.password=%2$s
                quarkus.tls.trust-store.p12.path=%1$s-server-truststore.p12
                quarkus.tls.trust-store.p12.password=%2$s
                quarkus.http.ssl.client-auth=%3$s
                quarkus.http.proxy.proxy-address-forwarding=true
                quarkus.http.proxy.allow-forwarded=true
                quarkus.http.proxy.allow-x-forwarded=true
                quarkus.http.proxy.enable-forwarded-host=true
                quarkus.http.proxy.enable-forwarded-prefix=true
                quarkus.http.proxy.enable-trusted-proxy-header=true
                """.formatted(CERTS_DIR + CERT_NAME, PASSWORD, clientAuth));
        for (int i = 0; i < trustedProxyDns.length; i++) {
            sb.append("quarkus.http.proxy.trusted-proxy-dns[").append(i).append("]=").append(trustedProxyDns[i])
                    .append("\n");
        }

        return new QuarkusExtensionTest()
                .withApplicationRoot(jar -> jar
                        .addClasses(ForwardedHandlerInitializer.class)
                        .addAsResource(new StringAsset(sb.toString()), "application.properties")
                        .addAsResource(new File(CERTS_DIR + CERT_NAME + "-keystore.p12"), "server-keystore.p12")
                        .addAsResource(new File(CERTS_DIR + CERT_NAME + "-server-truststore.p12"), "server-truststore.p12"));
    }

    final ValidatableResponse forwardedRequest() {
        return mTlsRequest()
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .get(tlsUrl)
                .then()
                .statusCode(200);
    }

    final ValidatableResponse xForwardedRequest() {
        return mTlsRequest()
                .header("X-Forwarded-Ssl", "on")
                .header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Host", "somehost")
                .get(tlsUrl)
                .then()
                .statusCode(200);
    }

    static void assertTrusted(ValidatableResponse response) {
        response.body(Matchers.equalTo("https|somehost|backend:4444|true"));
    }

    static void assertUntrusted(ValidatableResponse response) {
        response
                .body(Matchers.startsWith("https|localhost"))
                .body(Matchers.endsWith("|false"));
    }

    static RequestSpecification mTlsRequest() {
        return RestAssured.given()
                .keyStore(CERTS_DIR + CERT_NAME + "-client-keystore.p12", PASSWORD)
                .trustStore(CERTS_DIR + CERT_NAME + "-client-truststore.p12", PASSWORD);
    }
}
