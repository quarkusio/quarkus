package io.quarkus.vertx.http.proxy;

import static io.quarkus.vertx.http.proxy.AbstractTrustedProxyDnTest.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
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

@Certificates(certificates = {
        @Certificate(name = TrustedProxyPerEntryCheckTest.CERT_NAME, password = PASSWORD, formats = Format.PKCS12, subjectAlternativeNames = "DNS:localhost", client = true, aliases = {
                @Alias(name = "proxy-a", cn = "proxy-a", client = true, password = PASSWORD),
                @Alias(name = "proxy-b", cn = "proxy-b", client = true, password = PASSWORD)
        })
}, replaceIfExists = true, baseDir = "target/certs")
class TrustedProxyPerEntryCheckTest {

    static final String CERT_NAME = "proxy-per-entry-test";
    private static final String CERTS_DIR = "target/certs/";

    private static final String configuration = """
            quarkus.tls.key-store.p12.path=%1$s-keystore.p12
            quarkus.tls.key-store.p12.password=%2$s
            quarkus.tls.key-store.p12.alias=%3$s
            quarkus.tls.key-store.p12.alias-password=%2$s
            quarkus.tls.trust-store.p12.path=%1$s-server-truststore.p12
            quarkus.tls.trust-store.p12.password=%2$s
            quarkus.http.ssl.client-auth=REQUEST
            quarkus.http.proxy.proxy-address-forwarding=true
            quarkus.http.proxy.allow-forwarded=true
            quarkus.http.proxy.enable-trusted-proxy-header=true
            quarkus.http.proxy.trusted-proxy[0].subject-dn=CN=proxy-a
            quarkus.http.proxy.trusted-proxy[0].truststore-alias=proxy-b
            quarkus.http.proxy.trusted-proxy[1].subject-dn=CN=proxy-b
            quarkus.http.proxy.trusted-proxy[1].truststore-alias=proxy-a
            """.formatted(CERTS_DIR + CERT_NAME, PASSWORD, CERT_NAME);

    @TestHTTPResource(value = "/trusted-proxy", tls = true)
    URL tlsUrl;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File(CERTS_DIR + CERT_NAME + "-keystore.p12"), "server-keystore.p12")
                    .addAsResource(new File(CERTS_DIR + CERT_NAME + "-server-truststore.p12"),
                            "server-truststore.p12"));

    @Test
    void proxyARejectedWhenDnAndAliasMatchDifferentEntries() {
        String body = requestWithClientAlias("proxy-a");
        assertThat(body).startsWith("https|localhost").endsWith("|false");
    }

    @Test
    void proxyBRejectedWhenDnAndAliasMatchDifferentEntries() {
        String body = requestWithClientAlias("proxy-b");
        assertThat(body).startsWith("https|localhost").endsWith("|false");
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
