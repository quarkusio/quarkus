package io.quarkus.vertx.http.proxy;

import static io.quarkus.vertx.http.proxy.TrustedProxyCertChainHelper.buildPkcs12;
import static io.quarkus.vertx.http.proxy.TrustedProxyCertChainHelper.generateTwoChains;
import static io.quarkus.vertx.http.proxy.TrustedProxyCertChainHelper.requestWithClientKeystore;
import static io.quarkus.vertx.http.proxy.TrustedProxyCertChainHelper.writeKeyPem;
import static io.quarkus.vertx.http.proxy.TrustedProxyCertChainHelper.writePem;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.security.cert.X509Certificate;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.vertx.core.Vertx;

class TrustedProxyDnWithPemTest {

    private static final String CERTS_DIR = "target/proxy-dn-pem-test/";
    private static final File BASE_DIR = new File(CERTS_DIR);
    private static final File CLIENT1_KEYSTORE = new File(BASE_DIR, "client1-keystore.p12");
    private static final File CLIENT2_KEYSTORE = new File(BASE_DIR, "client2-keystore.p12");
    private static final File CLIENT_TRUSTSTORE = new File(BASE_DIR, "client-truststore.p12");

    private static final String configuration = """
            quarkus.tls.key-store.pem.0.cert=%1$sserver.crt
            quarkus.tls.key-store.pem.0.key=%1$sserver.key
            quarkus.tls.trust-store.pem.certs=%1$strusted-ca.crt,%1$simpostor-ca.crt
            quarkus.http.ssl.client-auth=REQUIRED
            quarkus.http.proxy.proxy-address-forwarding=true
            quarkus.http.proxy.allow-forwarded=true
            quarkus.http.proxy.enable-trusted-proxy-header=true
            quarkus.http.proxy.trusted-proxy[0].subject-dn=CN=proxy
            quarkus.http.proxy.trusted-proxy[0].truststore-alias=cert-0
            """.formatted(CERTS_DIR);

    @TestHTTPResource(value = "/trusted-proxy", tls = true)
    URL tlsUrl;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ForwardedHandlerInitializer.class, TrustedProxyCertChainHelper.class)
                    .addAsResource(new StringAsset(configuration), "application.properties"))
            .setBeforeAllCustomizer(() -> generateTwoChains(BASE_DIR,
                    (root1, fullChain1, leafKey1, root2, fullChain2, leafKey2) -> {
                        X509Certificate leaf1 = (X509Certificate) fullChain1[0];
                        X509Certificate inter1 = (X509Certificate) fullChain1[1];
                        writePem(new File(BASE_DIR, "server.crt"), new X509Certificate[] { leaf1, inter1, root1 });
                        writeKeyPem(new File(BASE_DIR, "server.key"), leafKey1);

                        writePem(new File(BASE_DIR, "trusted-ca.crt"), root1);
                        writePem(new File(BASE_DIR, "impostor-ca.crt"), root2);

                        buildPkcs12(CLIENT1_KEYSTORE, (ks, pass) -> ks.setKeyEntry("client", leafKey1, pass, fullChain1));
                        buildPkcs12(CLIENT2_KEYSTORE, (ks, pass) -> ks.setKeyEntry("client", leafKey2, pass, fullChain2));
                        buildPkcs12(CLIENT_TRUSTSTORE, (ks, pass) -> ks.setCertificateEntry("server-ca", root1));
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
}
