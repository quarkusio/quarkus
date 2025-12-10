package io.quarkus.it.vertx;

import java.util.Set;
import java.util.concurrent.CompletionException;

import javax.net.ssl.SSLHandshakeException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@QuarkusTest
public class TlsProtocolVersionDefaultTestCase {

    @TestHTTPResource(value = "/hello", tls = true)
    String url;

    @Inject
    Vertx vertx;

    @Test
    void testWithWebClientRequestingMultipleTlsVersions() {
        // The Web client is requesting TLS 1.2 or 1.3, the server is exposing 1.3 - all good
        WebClient client = WebClient.create(vertx, new WebClientOptions().setSsl(true)
                .setKeyStoreOptions(
                        new JksOptions().setPath("src/test/resources/client-keystore-1.jks").setPassword("password"))
                .setTrustStoreOptions(
                        new JksOptions().setPath("src/test/resources/client-truststore.jks").setPassword("password"))
                .setVerifyHost(false));
        var resp = client.getAbs(url).sendAndAwait();
        Assertions.assertEquals(200, resp.statusCode());
    }

    @Test
    void testWithWebClientRequestingTls13() {
        // The Web client is requesting TLS 1.3, the server is exposing 1.3 - all good
        WebClient client = WebClient.create(vertx, new WebClientOptions().setSsl(true)
                .setEnabledSecureTransportProtocols(Set.of("TLSv1.3"))
                .setKeyStoreOptions(
                        new JksOptions().setPath("src/test/resources/client-keystore-1.jks").setPassword("password"))
                .setTrustStoreOptions(
                        new JksOptions().setPath("src/test/resources/client-truststore.jks").setPassword("password"))
                .setVerifyHost(false));
        var resp = client.getAbs(url).sendAndAwait();
        Assertions.assertEquals(200, resp.statusCode());
    }

    @Test
    void testWithWebClientRequestingTls12() {
        // The Web client is requesting TLS 1.2, the server is exposing 1.3 - KO
        WebClient client = WebClient.create(vertx, new WebClientOptions().setSsl(true)
                .setEnabledSecureTransportProtocols(Set.of("TLSv1.2"))
                .setKeyStoreOptions(
                        new JksOptions().setPath("src/test/resources/client-keystore-1.jks").setPassword("password"))
                .setTrustStoreOptions(
                        new JksOptions().setPath("src/test/resources/client-truststore.jks").setPassword("password"))
                .setVerifyHost(false));
        Throwable exception = Assertions.assertThrows(CompletionException.class, () -> client.getAbs(url).sendAndAwait());
        Assertions.assertTrue(exception.getCause() instanceof SSLHandshakeException);
    }

    @Test
    void testWithWebClientRequestingTls11() {
        // The Web client is requesting TLS 1.1, the server is exposing 1.2 and 1.3 - KO
        WebClient client = WebClient.create(vertx, new WebClientOptions().setSsl(true)
                .setEnabledSecureTransportProtocols(Set.of("TLSv1.1"))
                .setKeyStoreOptions(
                        new JksOptions().setPath("src/test/resources/client-keystore-1.jks").setPassword("password"))
                .setTrustStoreOptions(
                        new JksOptions().setPath("src/test/resources/client-truststore.jks").setPassword("password"))
                .setVerifyHost(false));
        Throwable exception = Assertions.assertThrows(CompletionException.class, () -> client.getAbs(url).sendAndAwait());
        Assertions.assertTrue(exception.getCause() instanceof SSLHandshakeException);
    }
}
