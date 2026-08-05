package io.quarkus.vertx.http.proxy;

import java.net.URL;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

class TrustedProxyDnRequestTest extends AbstractTrustedProxyDnTest {

    @TestHTTPResource(value = "/trusted-proxy")
    URL httpUrl;

    @RegisterExtension
    static final QuarkusExtensionTest config = createTest("REQUEST", "CN=localhost");

    @Test
    void testForwardedWithMatchingDn() {
        assertTrusted(forwardedRequest());
    }

    @Test
    void testXForwardedWithMatchingDn() {
        assertTrusted(xForwardedRequest());
    }

    @Test
    void testHttpConnectionForwardedIgnored() {
        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .get(httpUrl)
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("http|localhost"))
                .body(Matchers.endsWith("|false"));
    }

    @Test
    void testHttpsWithoutClientCertForwardedIgnored() {
        RestAssured.given()
                .trustStore(CERTS_DIR + CERT_NAME + "-client-truststore.p12", PASSWORD)
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .get(tlsUrl)
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("https|localhost"))
                .body(Matchers.endsWith("|false"));
    }
}
