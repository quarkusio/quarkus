package io.quarkus.vertx.http.runtime.options;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyStore;
import java.security.KeyStoreSpi;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.vertx.http.runtime.CertificateConfig;
import io.quarkus.vertx.http.runtime.ServerSslConfig;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.ServerSSLOptions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TlsCertificateReloaderTest {

    @Mock
    Vertx vertx;

    @Mock
    HttpServer server;

    @Mock
    TlsConfigurationRegistry registry;

    private long lastTimerId = -1;

    @AfterEach
    void cleanup() {
        if (lastTimerId >= 0) {
            TlsCertificateReloader.unschedule(vertx, lastTimerId);
            lastTimerId = -1;
        }
    }

    @Test
    void initCertReloadingAction_periodBelow30Seconds_throws() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(29));
        HttpServerConfig config = new HttpServerConfig();
        ServerSSLOptions sslOptions = new ServerSSLOptions();
        sslOptions.setKeyCertOptions(new PemKeyCertOptions());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TlsCertificateReloader.initCertReloadingAction(
                        vertx, server, config, sslOptions, sslConfig, registry, Optional.empty(), ClientAuth.REQUIRED));

        assertEquals("Unable to configure TLS reloading - The reload period cannot be less than 30 seconds",
                ex.getMessage());
    }

    @Test
    void initCertReloadingAction_periodExactly30Seconds_isAccepted() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(30));
        HttpServerConfig config = new HttpServerConfig();
        ServerSSLOptions sslOptions = new ServerSSLOptions();
        sslOptions.setKeyCertOptions(new PemKeyCertOptions());

        when(vertx.setPeriodic(anyLong(), any(Handler.class))).thenReturn(42L);

        assertDoesNotThrow(() -> {
            lastTimerId = TlsCertificateReloader.initCertReloadingAction(
                    vertx, server, config, sslOptions, sslConfig, registry, Optional.empty(), ClientAuth.REQUIRED);
        });
    }

    @Test
    void initCertReloadingAction_periodOf1Second_throws() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(1));
        HttpServerConfig config = new HttpServerConfig();
        ServerSSLOptions sslOptions = new ServerSSLOptions();
        sslOptions.setKeyCertOptions(new PemKeyCertOptions());

        assertThrows(IllegalArgumentException.class,
                () -> TlsCertificateReloader.initCertReloadingAction(
                        vertx, server, config, sslOptions, sslConfig, registry, Optional.empty(), ClientAuth.REQUIRED));
    }

    @Test
    void initCertReloadingAction_noReloadPeriod_returnsMinusOne() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(null);
        HttpServerConfig config = new HttpServerConfig();
        ServerSSLOptions sslOptions = new ServerSSLOptions();
        sslOptions.setKeyCertOptions(new PemKeyCertOptions());

        long result = TlsCertificateReloader.initCertReloadingAction(
                vertx, server, config, sslOptions, sslConfig, registry, Optional.empty(), ClientAuth.REQUIRED);

        assertEquals(-1L, result);
    }

    @Test
    void initCertReloadingAction_nullConfig_throws() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(60));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TlsCertificateReloader.initCertReloadingAction(
                        vertx, server, null, null, sslConfig, registry, Optional.empty(), ClientAuth.REQUIRED));

        assertEquals("Unable to configure TLS reloading - The HTTP server configuration was not provided",
                ex.getMessage());
    }

    @Test
    void unschedule_cancelsTimerOnVertx() {
        TlsCertificateReloader.unschedule(vertx, 99L);

        verify(vertx).cancelTimer(99L);
    }

    @Test
    void initCertReloadingAction_withTlsRegistry_acceptsValidPeriod() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(60));
        HttpServerConfig config = new HttpServerConfig();

        TlsConfiguration tlsConfig = mock(TlsConfiguration.class);
        when(registry.get("my-tls")).thenReturn(Optional.of(tlsConfig));
        when(vertx.setPeriodic(anyLong(), any(Handler.class))).thenReturn(200L);

        lastTimerId = TlsCertificateReloader.initCertReloadingAction(
                vertx, server, config, null, sslConfig, registry, Optional.of("my-tls"), ClientAuth.REQUIRED);

        assertEquals(200L, lastTimerId);
    }

    @Test
    void initCertReloadingAction_returnsTimerId() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(60));
        HttpServerConfig config = new HttpServerConfig();
        ServerSSLOptions sslOptions = new ServerSSLOptions();
        sslOptions.setKeyCertOptions(new PemKeyCertOptions());

        when(vertx.setPeriodic(anyLong(), any(Handler.class))).thenReturn(555L);

        lastTimerId = TlsCertificateReloader.initCertReloadingAction(
                vertx, server, config, sslOptions, sslConfig, registry, Optional.empty(), ClientAuth.REQUIRED);

        assertEquals(555L, lastTimerId);
    }

    @Test
    void reloadFromRegistry_updatesServerOnlyWhenContentChanged() throws Exception {
        Vertx realVertx = Vertx.vertx();
        try {
            ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(60));
            TlsConfiguration tlsConfig = mock(TlsConfiguration.class);
            when(registry.get("my-tls")).thenReturn(Optional.of(tlsConfig));
            when(tlsConfig.reload()).thenReturn(true);
            when(tlsConfig.getServerSSLOptions()).thenAnswer(i -> new ServerSSLOptions());
            when(tlsConfig.getKeyStoreOptions()).thenReturn(pem("A"));
            when(server.updateSSLOptions(any(ServerSSLOptions.class))).thenReturn(Future.succeededFuture(true));

            ServerSSLOptions initial = HttpServerOptionsUtils.createServerSslOptions(tlsConfig, ClientAuth.NONE);
            lastTimerId = TlsCertificateReloader.initCertReloadingAction(realVertx, server, new HttpServerConfig(),
                    initial, sslConfig, registry, Optional.of("my-tls"), ClientAuth.NONE);

            // Same content: the server keeps its options
            TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);
            verify(server, never()).updateSSLOptions(any(ServerSSLOptions.class));

            // Rotated content: the server is updated once, and not again on the next unchanged reload
            when(tlsConfig.getKeyStoreOptions()).thenReturn(pem("B"));
            TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);
            TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);
            verify(server, times(1)).updateSSLOptions(any(ServerSSLOptions.class));
        } finally {
            TlsCertificateReloader.unschedule(realVertx, lastTimerId);
            lastTimerId = -1;
            realVertx.close().await();
        }
    }

    @Test
    void reloadFromRegistry_detectsRotationWhenOptionsDoNotEmbedContent() throws Exception {
        Vertx realVertx = Vertx.vertx();
        try {
            ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(60));
            TlsConfiguration tlsConfig = mock(TlsConfiguration.class);
            when(registry.get("my-tls")).thenReturn(Optional.of(tlsConfig));
            when(tlsConfig.reload()).thenReturn(true);
            when(tlsConfig.getServerSSLOptions()).thenAnswer(i -> new ServerSSLOptions());
            // Path-based options, equal before and after the rotation, like a custom KeyStoreFactory may return
            when(tlsConfig.getKeyStoreOptions()).thenReturn(new JksOptions().setPath("/certs/store.p12"));
            KeyStore before = keyStore(mock(Certificate.class));
            KeyStore after = keyStore(mock(Certificate.class));
            when(tlsConfig.getKeyStore()).thenReturn(before);
            when(server.updateSSLOptions(any(ServerSSLOptions.class))).thenReturn(Future.succeededFuture(true));

            ServerSSLOptions initial = HttpServerOptionsUtils.createServerSslOptions(tlsConfig, ClientAuth.NONE);
            lastTimerId = TlsCertificateReloader.initCertReloadingAction(realVertx, server, new HttpServerConfig(),
                    initial, sslConfig, registry, Optional.of("my-tls"), ClientAuth.NONE);

            TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);
            verify(server, never()).updateSSLOptions(any(ServerSSLOptions.class));

            // Same options, new certificate
            when(tlsConfig.getKeyStore()).thenReturn(after);
            TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);
            verify(server, times(1)).updateSSLOptions(any(ServerSSLOptions.class));
        } finally {
            TlsCertificateReloader.unschedule(realVertx, lastTimerId);
            lastTimerId = -1;
            realVertx.close().await();
        }
    }

    @Test
    void reloadFromRegistry_detectsReissuedIntermediate() throws Exception {
        Vertx realVertx = Vertx.vertx();
        try {
            ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(60));
            TlsConfiguration tlsConfig = mock(TlsConfiguration.class);
            when(registry.get("my-tls")).thenReturn(Optional.of(tlsConfig));
            when(tlsConfig.reload()).thenReturn(true);
            when(tlsConfig.getServerSSLOptions()).thenAnswer(i -> new ServerSSLOptions());
            when(tlsConfig.getKeyStoreOptions()).thenReturn(new JksOptions().setPath("/certs/store.p12"));
            Certificate leaf = mock(Certificate.class);
            KeyStore before = keyStore(leaf, mock(Certificate.class));
            KeyStore after = keyStore(leaf, mock(Certificate.class));
            when(tlsConfig.getKeyStore()).thenReturn(before);
            when(server.updateSSLOptions(any(ServerSSLOptions.class))).thenReturn(Future.succeededFuture(true));

            ServerSSLOptions initial = HttpServerOptionsUtils.createServerSslOptions(tlsConfig, ClientAuth.NONE);
            lastTimerId = TlsCertificateReloader.initCertReloadingAction(realVertx, server, new HttpServerConfig(),
                    initial, sslConfig, registry, Optional.of("my-tls"), ClientAuth.NONE);

            TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);
            verify(server, never()).updateSSLOptions(any(ServerSSLOptions.class));

            // Same options, same leaf, new intermediate
            when(tlsConfig.getKeyStore()).thenReturn(after);
            TlsCertificateReloader.reload().toCompletableFuture().get(10, TimeUnit.SECONDS);
            verify(server, times(1)).updateSSLOptions(any(ServerSSLOptions.class));
        } finally {
            TlsCertificateReloader.unschedule(realVertx, lastTimerId);
            lastTimerId = -1;
            realVertx.close().await();
        }
    }

    /**
     * A single certificate is a trusted entry (no chain), several are the chain of a key entry, leaf first.
     */
    private static KeyStore keyStore(Certificate... chain) throws Exception {
        KeyStoreSpi spi = mock(KeyStoreSpi.class);
        when(spi.engineAliases()).thenAnswer(i -> Collections.enumeration(List.of("alias")));
        when(spi.engineGetCertificate("alias")).thenReturn(chain[0]);
        if (chain.length > 1) {
            when(spi.engineGetCertificateChain("alias")).thenReturn(chain);
        }
        KeyStore store = new KeyStore(spi, null, "test") {
        };
        store.load(null, null);
        return store;
    }

    private static PemKeyCertOptions pem(String content) {
        return new PemKeyCertOptions().addCertValue(Buffer.buffer(content)).addKeyValue(Buffer.buffer(content));
    }

    private ServerSslConfig mockSslConfigWithReloadPeriod(Duration reloadPeriod) {
        ServerSslConfig sslConfig = mock(ServerSslConfig.class);
        CertificateConfig certConfig = mock(CertificateConfig.class);
        when(sslConfig.certificate()).thenReturn(certConfig);
        if (reloadPeriod != null) {
            when(certConfig.reloadPeriod()).thenReturn(Optional.of(reloadPeriod));
        } else {
            when(certConfig.reloadPeriod()).thenReturn(Optional.empty());
        }
        return sslConfig;
    }
}
