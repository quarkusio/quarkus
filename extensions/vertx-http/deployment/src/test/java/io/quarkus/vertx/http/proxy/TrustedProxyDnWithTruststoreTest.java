package io.quarkus.vertx.http.proxy;

import static io.quarkus.vertx.http.proxy.AbstractTrustedProxyDnTest.PASSWORD;
import static io.quarkus.vertx.http.proxy.TrustedProxyCertChainHelper.buildPkcs12;
import static io.quarkus.vertx.http.proxy.TrustedProxyCertChainHelper.generateTwoChains;
import static io.quarkus.vertx.http.proxy.TrustedProxyCertChainHelper.requestWithClientKeystore;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.Collections;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;

class TrustedProxyDnWithTruststoreTest {

    private static final String CERTS_DIR = "target/proxy-dn-ts-test/";
    private static final File BASE_DIR = new File(CERTS_DIR);
    private static final File SERVER_KEYSTORE = new File(BASE_DIR, "server-keystore.p12");
    private static final File SERVER_TRUSTSTORE = new File(BASE_DIR, "server-truststore.p12");
    private static final File CLIENT1_KEYSTORE = new File(BASE_DIR, "client1-keystore.p12");
    private static final File CLIENT2_KEYSTORE = new File(BASE_DIR, "client2-keystore.p12");
    private static final File CLIENT_TRUSTSTORE = new File(BASE_DIR, "client-truststore.p12");
    private static final String TRUSTED_CA = "trusted-ca";

    private static final String configuration = """
            quarkus.tls.http-server.key-store.p12.path=%1$sserver-keystore.p12
            quarkus.tls.http-server.key-store.p12.password=%2$s
            quarkus.tls.http-server.trust-store.p12.path=%1$sserver-truststore.p12
            quarkus.tls.http-server.trust-store.p12.password=%2$s
            quarkus.http.tls-configuration-name=http-server
            quarkus.http.ssl.client-auth=REQUEST
            quarkus.http.proxy.proxy-address-forwarding=true
            quarkus.http.proxy.allow-forwarded=true
            quarkus.http.proxy.enable-trusted-proxy-header=true
            quarkus.http.proxy.trusted-proxy[0].subject-dn=CN=proxy
            quarkus.http.proxy.trusted-proxy[0].truststore-alias=%3$s
            """.formatted(CERTS_DIR, PASSWORD, TRUSTED_CA);

    @TestHTTPResource(value = "/trusted-proxy", tls = true)
    URL tlsUrl;

    @TestHTTPResource(value = "/trusted-proxy")
    URL httpUrl;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ForwardedHandlerInitializer.class, TrustedProxyCertChainHelper.class)
                    .addAsResource(new StringAsset(configuration), "application.properties"))
            .setBeforeAllCustomizer(() -> generateTwoChains(BASE_DIR,
                    (root1, fullChain1, leafKey1, root2, fullChain2, leafKey2) -> {
                        buildPkcs12(SERVER_KEYSTORE, (ks, pass) -> ks.setKeyEntry("server", leafKey1, pass, fullChain1));

                        buildPkcs12(SERVER_TRUSTSTORE, (ks, pass) -> {
                            ks.setCertificateEntry(TRUSTED_CA, root1);
                            ks.setCertificateEntry("impostor-ca", root2);
                        });

                        buildPkcs12(CLIENT1_KEYSTORE, (ks, pass) -> ks.setKeyEntry("client", leafKey1, pass, fullChain1));
                        buildPkcs12(CLIENT2_KEYSTORE, (ks, pass) -> ks.setKeyEntry("client", leafKey2, pass, fullChain2));
                        buildPkcs12(CLIENT_TRUSTSTORE, (ks, pass) -> ks.setCertificateEntry("server-ca", root1));

                        KeyStore verify = KeyStore.getInstance("PKCS12");
                        try (FileInputStream fis = new FileInputStream(SERVER_TRUSTSTORE)) {
                            verify.load(fis, PASSWORD.toCharArray());
                        }
                        assertThat(Collections.list(verify.aliases()))
                                .containsExactlyInAnyOrder(TRUSTED_CA, "impostor-ca");
                    }));

    @Test
    void trustedProxyForwardedHeadersHonored() {
        String body = requestWithClientKeystore(vertx, tlsUrl, CLIENT1_KEYSTORE, CLIENT_TRUSTSTORE);
        assertThat(body).isEqualTo("https|somehost|backend:4444|true");
    }

    @Test
    void impostorWithSameDnButWrongCaForwardedHeadersIgnored() {
        String body = requestWithClientKeystore(vertx, tlsUrl, CLIENT2_KEYSTORE, CLIENT_TRUSTSTORE);
        assertThat(body).startsWith("https|localhost").endsWith("|false");
    }

    @Test
    void httpsWithoutClientCertForwardedHeadersIgnored() {
        RestAssured.given()
                .trustStore(CLIENT_TRUSTSTORE.getPath(), PASSWORD)
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .get(tlsUrl)
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("https|localhost"))
                .body(Matchers.endsWith("|false"));
    }

    @Test
    void httpConnectionForwardedHeadersIgnored() {
        RestAssured.given()
                .header("Forwarded", "proto=https;for=backend:4444;host=somehost")
                .get(httpUrl)
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("http|localhost"))
                .body(Matchers.endsWith("|false"));
    }
}
