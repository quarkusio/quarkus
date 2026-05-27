package io.quarkus.vertx.http.proxy;

import static io.quarkus.vertx.http.proxy.AbstractTrustedProxyDnTest.PASSWORD;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyStore;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PfxOptions;

@Disabled("https://github.com/quarkusio/quarkus/issues/55130") // TODO: enable when #55130 is fixed
@Certificates(certificates = {
        @Certificate(subjectAlternativeNames = "DNS:localhost", client = true, aliases = {
                @Alias(name = "proxy-a", cn = "proxy-a", client = true, password = PASSWORD),
                @Alias(name = "proxy-b", cn = "proxy-b", client = true, password = PASSWORD)
        }, name = TrustedProxyTruststoreReloadTest.CERT_NAME, password = PASSWORD, formats = Format.PKCS12)
}, replaceIfExists = true, baseDir = "target/certs")
@DisabledOnOs(OS.WINDOWS)
class TrustedProxyTruststoreReloadTest {

    static final String CERT_NAME = "proxy-reload-test";
    private static final String CERTS_DIR = "target/certs/";
    private static final String PROXY_ALIAS = "proxy";
    private static final File TEMP_DIR = new File("target/certs/proxy-reload-tmp").getAbsoluteFile();

    private static final String configuration = """
            quarkus.tls.key-store.p12.path=%1$s-keystore.p12
            quarkus.tls.key-store.p12.password=%2$s
            quarkus.tls.key-store.p12.alias=%3$s
            quarkus.tls.key-store.p12.alias-password=%2$s
            quarkus.tls.trust-store.p12.path=%4$s/server-truststore.p12
            quarkus.tls.trust-store.p12.password=%2$s
            quarkus.tls.reload-period=1s
            quarkus.http.ssl.client-auth=REQUEST
            quarkus.http.proxy.proxy-address-forwarding=true
            quarkus.http.proxy.allow-forwarded=true
            quarkus.http.proxy.enable-trusted-proxy-header=true
            quarkus.http.proxy.trusted-proxy[0].subject-dn=CN=proxy-a
            quarkus.http.proxy.trusted-proxy[0].truststore-alias=%5$s
            quarkus.http.proxy.trusted-proxy[1].subject-dn=CN=proxy-b
            quarkus.http.proxy.trusted-proxy[1].truststore-alias=%5$s
            """.formatted(CERTS_DIR + CERT_NAME, PASSWORD, CERT_NAME, TEMP_DIR.getAbsolutePath(), PROXY_ALIAS);

    @TestHTTPResource(value = "/trusted-proxy", tls = true)
    URL tlsUrl;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File(CERTS_DIR + CERT_NAME + "-keystore.p12"), "server-keystore.p12"))
            .setBeforeAllCustomizer(() -> {
                try {
                    TEMP_DIR.mkdirs();
                    buildTruststore("proxy-a");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .setAfterAllCustomizer(() -> {
                try {
                    Files.deleteIfExists(new File(TEMP_DIR, "server-truststore.p12").toPath());
                    Files.deleteIfExists(TEMP_DIR.toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Test
    void truststoreReloadChangesProxyTrust() throws Exception {
        // Before reload: proxy-a is trusted, proxy-b is not
        assertThat(requestWithClientAlias("proxy-a")).startsWith("https|somehost|").endsWith("|true");
        assertThat(requestWithClientAlias("proxy-b")).startsWith("https|localhost").endsWith("|false");

        // Swap truststore: replace proxy-a's cert with proxy-b's cert under the same alias
        buildTruststore("proxy-b");

        // Wait for periodic reload to pick up the change
        await().atMost(10, SECONDS).untilAsserted(() -> {
            assertThat(requestWithClientAlias("proxy-b")).startsWith("https|somehost|").endsWith("|true");
        });
        assertThat(requestWithClientAlias("proxy-a")).startsWith("https|localhost").endsWith("|false");

        // Swap truststore to one with no proxy alias — proxy mustn't be trusted
        buildTruststore(null);

        await().atMost(10, SECONDS).untilAsserted(() -> {
            assertThat(requestWithClientAlias("proxy-b")).startsWith("https|localhost").endsWith("|false");
        });
        assertThat(requestWithClientAlias("proxy-a")).startsWith("https|localhost").endsWith("|false");
    }

    private static void buildTruststore(String proxyAliasSource) throws Exception {
        char[] pass = PASSWORD.toCharArray();
        KeyStore source = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(CERTS_DIR + CERT_NAME + "-server-truststore.p12")) {
            source.load(fis, pass);
        }
        KeyStore target = KeyStore.getInstance("PKCS12");
        target.load(null, null);
        if (proxyAliasSource != null) {
            target.setCertificateEntry(PROXY_ALIAS, source.getCertificate(proxyAliasSource));
        } else {
            target.setCertificateEntry("proxy-a", source.getCertificate("proxy-a"));
            target.setCertificateEntry("proxy-b", source.getCertificate("proxy-b"));
        }
        try (FileOutputStream fos = new FileOutputStream(new File(TEMP_DIR, "server-truststore.p12"))) {
            target.store(fos, pass);
        }
    }

    private String requestWithClientAlias(String alias) {
        var options = new HttpClientOptions()
                .setSsl(true)
                .setDefaultPort(tlsUrl.getPort())
                .setDefaultHost(tlsUrl.getHost())
                .setKeyCertOptions(
                        new PfxOptions()
                                .setPath(CERTS_DIR + CERT_NAME + "-client-keystore.p12")
                                .setPassword(PASSWORD)
                                .setAlias(alias))
                .setTrustOptions(
                        new PfxOptions()
                                .setPath(CERTS_DIR + CERT_NAME + "-client-truststore.p12")
                                .setPassword(PASSWORD));

        var client = vertx.createHttpClient(options);
        try {
            return client
                    .request(HttpMethod.GET, "/trusted-proxy")
                    .map(req -> req.putHeader("Forwarded", "proto=https;for=backend:4444;host=somehost"))
                    .flatMap(HttpClientRequest::send)
                    .flatMap(HttpClientResponse::body)
                    .map(Buffer::toString)
                    .toCompletionStage().toCompletableFuture().join();
        } finally {
            client.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
