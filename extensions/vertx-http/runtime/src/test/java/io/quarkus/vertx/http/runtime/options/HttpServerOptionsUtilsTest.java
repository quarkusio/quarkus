package io.quarkus.vertx.http.runtime.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.vertx.http.runtime.ProxyConfig;
import io.quarkus.vertx.http.runtime.ServerLimitsConfig;
import io.quarkus.vertx.http.runtime.TrafficShapingConfig;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig.InsecureRequests;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;

class HttpServerOptionsUtilsTest {

    @Test
    void applyCommonOptionsWithGzipCompressor() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(true, Optional.of(List.of("gzip")), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getCompressors()).isNotNull();
        assertThat(options.getCompressors()).hasSize(1);
        assertThat(options.isCompressionSupported()).isTrue();
    }

    @Test
    void applyCommonOptionsWithDeflateCompressor() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(true, Optional.of(List.of("deflate")), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getCompressors()).isNotNull();
        assertThat(options.getCompressors()).hasSize(1);
    }

    @Test
    void applyCommonOptionsWithGzipAndDeflateCompressors() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(true, Optional.of(List.of("gzip", "deflate")),
                OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getCompressors()).hasSize(2);
    }

    @Test
    void applyCommonOptionsWithoutCompressors() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(true, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.isCompressionSupported()).isTrue();
        assertThat(options.getCompressors()).isNull();
    }

    @Test
    void applyCommonOptionsCompressionDisabled() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.isCompressionSupported()).isFalse();
    }

    @Test
    void applyCommonOptionsCompressionLevelApplied() {
        HttpServerOptions options = new HttpServerOptions();
        int level = 3;
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(true, Optional.empty(), OptionalInt.of(level));
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getCompressionLevel()).isEqualTo(level);
    }

    @Test
    void applyCommonOptionsCompressionLevelAppliedToGzipCompressor() {
        HttpServerOptions options = new HttpServerOptions();
        int level = 4;
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(true, Optional.of(List.of("gzip")), OptionalInt.of(level));
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getCompressionLevel()).isEqualTo(level);
        assertThat(options.getCompressors()).hasSize(1);
    }

    @Test
    void applyCommonOptionsHttp2SettingsApplied() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig(true);

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getInitialSettings()).isNotNull();
        assertThat(options.getInitialSettings().isPushEnabled()).isTrue();
    }

    @Test
    void applyCommonOptionsHttp2Disabled() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig(false);

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.isHttp2ClearTextEnabled()).isFalse();
    }

    @Test
    void applyCommonOptionsTrafficShapingEnabled() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        TrafficShapingConfig trafficShaping = mock(TrafficShapingConfig.class);
        when(trafficShaping.enabled()).thenReturn(true);
        when(trafficShaping.checkInterval()).thenReturn(Optional.of(Duration.ofSeconds(10)));
        when(trafficShaping.maxDelay()).thenReturn(Optional.of(Duration.ofSeconds(15)));
        when(trafficShaping.inboundGlobalBandwidth()).thenReturn(Optional.empty());
        when(trafficShaping.outboundGlobalBandwidth()).thenReturn(Optional.empty());
        when(trafficShaping.peakOutboundGlobalBandwidth()).thenReturn(Optional.empty());
        when(httpConfig.trafficShaping()).thenReturn(trafficShaping);

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getTrafficShapingOptions()).isNotNull();
    }

    @Test
    void applyCommonOptionsTrafficShapingDisabled() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getTrafficShapingOptions()).isNull();
    }

    @Test
    void applyCommonOptionsSetsServerLimits() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(options.getMaxHeaderSize()).isEqualTo(20480);
        assertThat(options.getMaxChunkSize()).isEqualTo(8192);
        assertThat(options.getMaxFormAttributeSize()).isEqualTo(2048);
        assertThat(options.getMaxFormFields()).isEqualTo(256);
        assertThat(options.getMaxInitialLineLength()).isEqualTo(4096);
    }

    @Test
    void applyCommonOptionsSetsWebsocketSubProtocols() {
        HttpServerOptions options = new HttpServerOptions();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();
        List<String> subProtocols = List.of("graphql-ws", "subscriptions-transport-ws");

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, subProtocols);

        assertThat(options.getWebSocketSubProtocols()).containsExactlyElementsOf(subProtocols);
    }

    @Test
    void getInsecureRequestStrategyReturnsDisabledWhenTlsClientAuthRequired() {
        VertxHttpConfig httpConfig = mock(VertxHttpConfig.class);
        VertxHttpBuildTimeConfig buildTimeConfig = mock(VertxHttpBuildTimeConfig.class);
        when(httpConfig.insecureRequests()).thenReturn(Optional.empty());

        try (MockedStatic<HttpServerTlsConfig> mocked = Mockito.mockStatic(HttpServerTlsConfig.class)) {
            mocked.when(() -> HttpServerTlsConfig.getTlsClientAuth(httpConfig, buildTimeConfig, LaunchMode.NORMAL))
                    .thenReturn(ClientAuth.REQUIRED);

            InsecureRequests result = HttpServerOptionsUtils.getInsecureRequestStrategy(httpConfig, buildTimeConfig,
                    LaunchMode.NORMAL);

            assertThat(result).isEqualTo(InsecureRequests.DISABLED);
        }
    }

    @Test
    void getInsecureRequestStrategyReturnsRedirectWhenConfigSaysRedirect() {
        VertxHttpConfig httpConfig = mock(VertxHttpConfig.class);
        VertxHttpBuildTimeConfig buildTimeConfig = mock(VertxHttpBuildTimeConfig.class);
        when(httpConfig.insecureRequests()).thenReturn(Optional.of(InsecureRequests.REDIRECT));

        try (MockedStatic<HttpServerTlsConfig> mocked = Mockito.mockStatic(HttpServerTlsConfig.class)) {
            mocked.when(() -> HttpServerTlsConfig.getTlsClientAuth(httpConfig, buildTimeConfig, LaunchMode.NORMAL))
                    .thenReturn(ClientAuth.NONE);

            InsecureRequests result = HttpServerOptionsUtils.getInsecureRequestStrategy(httpConfig, buildTimeConfig,
                    LaunchMode.NORMAL);

            assertThat(result).isEqualTo(InsecureRequests.REDIRECT);
        }
    }

    @Test
    void getInsecureRequestStrategyReturnsDisabledWhenConfigSaysDisabled() {
        VertxHttpConfig httpConfig = mock(VertxHttpConfig.class);
        VertxHttpBuildTimeConfig buildTimeConfig = mock(VertxHttpBuildTimeConfig.class);
        when(httpConfig.insecureRequests()).thenReturn(Optional.of(InsecureRequests.DISABLED));

        try (MockedStatic<HttpServerTlsConfig> mocked = Mockito.mockStatic(HttpServerTlsConfig.class)) {
            mocked.when(() -> HttpServerTlsConfig.getTlsClientAuth(httpConfig, buildTimeConfig, LaunchMode.NORMAL))
                    .thenReturn(ClientAuth.NONE);

            InsecureRequests result = HttpServerOptionsUtils.getInsecureRequestStrategy(httpConfig, buildTimeConfig,
                    LaunchMode.NORMAL);

            assertThat(result).isEqualTo(InsecureRequests.DISABLED);
        }
    }

    @Test
    void getInsecureRequestStrategyReturnsEnabledWhenConfigSaysEnabled() {
        VertxHttpConfig httpConfig = mock(VertxHttpConfig.class);
        VertxHttpBuildTimeConfig buildTimeConfig = mock(VertxHttpBuildTimeConfig.class);
        when(httpConfig.insecureRequests()).thenReturn(Optional.of(InsecureRequests.ENABLED));

        try (MockedStatic<HttpServerTlsConfig> mocked = Mockito.mockStatic(HttpServerTlsConfig.class)) {
            mocked.when(() -> HttpServerTlsConfig.getTlsClientAuth(httpConfig, buildTimeConfig, LaunchMode.NORMAL))
                    .thenReturn(ClientAuth.NONE);

            InsecureRequests result = HttpServerOptionsUtils.getInsecureRequestStrategy(httpConfig, buildTimeConfig,
                    LaunchMode.NORMAL);

            assertThat(result).isEqualTo(InsecureRequests.ENABLED);
        }
    }

    @Test
    void getInsecureRequestStrategyDefaultsToEnabledWhenNoClientAuthRequired() {
        VertxHttpConfig httpConfig = mock(VertxHttpConfig.class);
        VertxHttpBuildTimeConfig buildTimeConfig = mock(VertxHttpBuildTimeConfig.class);
        when(httpConfig.insecureRequests()).thenReturn(Optional.empty());

        try (MockedStatic<HttpServerTlsConfig> mocked = Mockito.mockStatic(HttpServerTlsConfig.class)) {
            mocked.when(() -> HttpServerTlsConfig.getTlsClientAuth(httpConfig, buildTimeConfig, LaunchMode.NORMAL))
                    .thenReturn(ClientAuth.NONE);

            InsecureRequests result = HttpServerOptionsUtils.getInsecureRequestStrategy(httpConfig, buildTimeConfig,
                    LaunchMode.NORMAL);

            assertThat(result).isEqualTo(InsecureRequests.ENABLED);
        }
    }

    @Test
    void getFileContentFromFilesystem(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test-file.txt");
        byte[] expected = "hello from filesystem".getBytes();
        Files.write(file, expected);

        byte[] result = HttpServerOptionsUtils.getFileContent(file);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getFileContentFromClasspath() throws IOException {
        // META-INF/MANIFEST.MF is a resource commonly available on the classpath
        Path path = Path.of("META-INF/MANIFEST.MF");

        byte[] result = HttpServerOptionsUtils.getFileContent(path);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    private VertxHttpBuildTimeConfig buildTimeConfig(boolean enableCompression,
            Optional<List<String>> compressors,
            OptionalInt compressionLevel) {
        VertxHttpBuildTimeConfig config = mock(VertxHttpBuildTimeConfig.class);
        when(config.enableCompression()).thenReturn(enableCompression);
        when(config.enableDecompression()).thenReturn(false);
        when(config.compressors()).thenReturn(compressors);
        when(config.compressionLevel()).thenReturn(compressionLevel);
        return config;
    }

    private VertxHttpConfig minimalHttpConfig() {
        return minimalHttpConfig(true);
    }

    private VertxHttpConfig minimalHttpConfig(boolean http2Enabled) {
        VertxHttpConfig config = mock(VertxHttpConfig.class);

        when(config.host()).thenReturn("localhost");
        when(config.idleTimeout()).thenReturn(Duration.ofMinutes(30));
        when(config.http2()).thenReturn(http2Enabled);
        when(config.http2PushEnabled()).thenReturn(true);
        when(config.http2ConnectionWindowSize()).thenReturn(OptionalInt.empty());
        when(config.initialWindowSize()).thenReturn(OptionalInt.empty());
        when(config.soReusePort()).thenReturn(false);
        when(config.tcpQuickAck()).thenReturn(false);
        when(config.tcpCork()).thenReturn(false);
        when(config.tcpFastOpen()).thenReturn(false);
        when(config.acceptBacklog()).thenReturn(-1);
        when(config.handle100ContinueAutomatically()).thenReturn(false);

        ServerLimitsConfig limits = mock(ServerLimitsConfig.class);
        when(limits.maxHeaderSize()).thenReturn(new MemorySize(BigInteger.valueOf(20480)));
        when(limits.maxChunkSize()).thenReturn(new MemorySize(BigInteger.valueOf(8192)));
        when(limits.maxFormAttributeSize()).thenReturn(new MemorySize(BigInteger.valueOf(2048)));
        when(limits.maxFormFields()).thenReturn(256);
        when(limits.maxFormBufferedBytes()).thenReturn(new MemorySize(BigInteger.valueOf(1024)));
        when(limits.maxInitialLineLength()).thenReturn(4096);
        when(limits.headerTableSize()).thenReturn(OptionalLong.empty());
        when(limits.maxConcurrentStreams()).thenReturn(OptionalLong.empty());
        when(limits.maxFrameSize()).thenReturn(OptionalInt.empty());
        when(limits.maxHeaderListSize()).thenReturn(OptionalLong.empty());
        when(limits.rstFloodMaxRstFramePerWindow()).thenReturn(OptionalInt.empty());
        when(limits.rstFloodWindowDuration()).thenReturn(Optional.empty());
        when(config.limits()).thenReturn(limits);

        TrafficShapingConfig trafficShaping = mock(TrafficShapingConfig.class);
        when(trafficShaping.enabled()).thenReturn(false);
        when(config.trafficShaping()).thenReturn(trafficShaping);

        ProxyConfig proxy = mock(ProxyConfig.class);
        when(proxy.useProxyProtocol()).thenReturn(false);
        when(config.proxy()).thenReturn(proxy);

        return config;
    }
}
