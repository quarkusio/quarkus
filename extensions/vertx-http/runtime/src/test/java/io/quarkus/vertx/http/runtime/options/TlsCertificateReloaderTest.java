package io.quarkus.vertx.http.runtime.options;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

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
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

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
        HttpServerOptions options = new HttpServerOptions();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TlsCertificateReloader.initCertReloadingAction(
                        vertx, server, options, sslConfig, registry, Optional.empty()));

        assertEquals("Unable to configure TLS reloading - The reload period cannot be less than 30 seconds",
                ex.getMessage());
    }

    @Test
    void initCertReloadingAction_periodExactly30Seconds_isAccepted() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(30));
        HttpServerOptions options = new HttpServerOptions();
        options.setSsl(true);
        options.setKeyCertOptions(new io.vertx.core.net.PemKeyCertOptions());

        when(vertx.setPeriodic(anyLong(), any(Handler.class))).thenReturn(42L);

        assertDoesNotThrow(() -> {
            lastTimerId = TlsCertificateReloader.initCertReloadingAction(
                    vertx, server, options, sslConfig, registry, Optional.empty());
        });
    }

    @Test
    void initCertReloadingAction_periodOf1Second_throws() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(1));
        HttpServerOptions options = new HttpServerOptions();

        assertThrows(IllegalArgumentException.class,
                () -> TlsCertificateReloader.initCertReloadingAction(
                        vertx, server, options, sslConfig, registry, Optional.empty()));
    }

    @Test
    void initCertReloadingAction_noReloadPeriod_returnsMinusOne() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(null);
        HttpServerOptions options = new HttpServerOptions();

        long result = TlsCertificateReloader.initCertReloadingAction(
                vertx, server, options, sslConfig, registry, Optional.empty());

        assertEquals(-1L, result);
    }

    @Test
    void initCertReloadingAction_nullOptions_throws() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(60));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TlsCertificateReloader.initCertReloadingAction(
                        vertx, server, null, sslConfig, registry, Optional.empty()));

        assertEquals("Unable to configure TLS reloading - The HTTP server options were not provided",
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
        HttpServerOptions options = new HttpServerOptions();

        TlsConfiguration tlsConfig = mock(TlsConfiguration.class);
        when(registry.get("my-tls")).thenReturn(Optional.of(tlsConfig));
        when(vertx.setPeriodic(anyLong(), any(Handler.class))).thenReturn(200L);

        lastTimerId = TlsCertificateReloader.initCertReloadingAction(
                vertx, server, options, sslConfig, registry, Optional.of("my-tls"));

        assertEquals(200L, lastTimerId);
    }

    @Test
    void initCertReloadingAction_returnsTimerId() {
        ServerSslConfig sslConfig = mockSslConfigWithReloadPeriod(Duration.ofSeconds(60));
        HttpServerOptions options = new HttpServerOptions();
        options.setSsl(true);
        options.setKeyCertOptions(new io.vertx.core.net.PemKeyCertOptions());

        when(vertx.setPeriodic(anyLong(), any(Handler.class))).thenReturn(555L);

        lastTimerId = TlsCertificateReloader.initCertReloadingAction(
                vertx, server, options, sslConfig, registry, Optional.empty());

        assertEquals(555L, lastTimerId);
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
