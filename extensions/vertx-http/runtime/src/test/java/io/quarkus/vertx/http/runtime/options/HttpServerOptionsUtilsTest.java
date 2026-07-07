package io.quarkus.vertx.http.runtime.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import io.quarkus.vertx.http.runtime.WebsocketServerConfig;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerConfig;

class HttpServerOptionsUtilsTest {

    @Test
    void applyCommonOptionsNewApiWithGzipCompressor() {
        HttpServerConfig config = new HttpServerConfig();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(true, Optional.of(List.of("gzip")), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(config, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(config.getCompressionConfig()).isNotNull();
        assertThat(config.getCompressionConfig().isCompressionEnabled()).isTrue();
    }

    @Test
    void applyCommonOptionsNewApiCompressionDisabled() {
        HttpServerConfig config = new HttpServerConfig();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(config, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(config.getCompressionConfig().isCompressionEnabled()).isFalse();
    }

    @Test
    void applyCommonOptionsNewApiHttp2SettingsApplied() {
        HttpServerConfig config = new HttpServerConfig();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig(true);

        HttpServerOptionsUtils.applyCommonOptions(config, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(config.getHttp2Config()).isNotNull();
        assertThat(config.getHttp2Config().getInitialSettings()).isNotNull();
        assertThat(config.getHttp2Config().getInitialSettings().isPushEnabled()).isTrue();
        assertThat(config.getHttp2Config().getInitialSettings().getMaxConcurrentStreams()).isEqualTo(200L);
    }

    @Test
    void applyCommonOptionsNewApiHttp2MaxConcurrentStreamsConfigured() {
        HttpServerConfig config = new HttpServerConfig();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig(true);
        ServerLimitsConfig limits = httpConfig.limits();
        when(limits.maxConcurrentStreams()).thenReturn(OptionalLong.of(500L));

        HttpServerOptionsUtils.applyCommonOptions(config, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(config.getHttp2Config().getInitialSettings().getMaxConcurrentStreams()).isEqualTo(500L);
    }

    @Test
    void applyCommonOptionsNewApiSetsServerLimits() {
        HttpServerConfig config = new HttpServerConfig();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();

        HttpServerOptionsUtils.applyCommonOptions(config, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(config.getHttp1Config().getMaxHeaderSize()).isEqualTo(20480);
        assertThat(config.getHttp1Config().getMaxChunkSize()).isEqualTo(8192);
        assertThat(config.getFormDecoderConfig().getMaxAttributeSize()).isEqualTo(2048);
        assertThat(config.getFormDecoderConfig().getMaxFields()).isEqualTo(256);
        assertThat(config.getHttp1Config().getMaxInitialLineLength()).isEqualTo(4096);
    }

    @Test
    void applyCommonOptionsNewApiSetsWebsocketSubProtocols() {
        HttpServerConfig config = new HttpServerConfig();
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();
        List<String> subProtocols = List.of("graphql-ws", "subscriptions-transport-ws");

        HttpServerOptionsUtils.applyCommonOptions(config, buildTimeConfig, httpConfig, subProtocols);

        assertThat(config.getWebSocketConfig().getSubProtocols()).containsExactlyElementsOf(subProtocols);
    }

    @Test
    void applyCommonOptionsNewApiTrafficShapingEnabled() {
        HttpServerConfig config = new HttpServerConfig();
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

        HttpServerOptionsUtils.applyCommonOptions(config, buildTimeConfig, httpConfig, Collections.emptyList());

        assertThat(config.getTcpConfig().getTrafficShapingOptions()).isNotNull();
    }

    @Test
    void createHttpServerConfigSetsPort() {
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();
        when(httpConfig.hostEnabled()).thenReturn(true);
        when(httpConfig.determinePort(LaunchMode.NORMAL)).thenReturn(8080);

        HttpServerConfig config = HttpServerOptionsUtils.createHttpServerConfig(
                buildTimeConfig, httpConfig, LaunchMode.NORMAL, Collections.emptyList());

        assertThat(config).isNotNull();
        assertThat(config.getTcpPort()).isEqualTo(8080);
    }

    @Test
    void createHttpServerConfigReturnsNullWhenHostDisabled() {
        VertxHttpBuildTimeConfig buildTimeConfig = buildTimeConfig(false, Optional.empty(), OptionalInt.empty());
        VertxHttpConfig httpConfig = minimalHttpConfig();
        when(httpConfig.hostEnabled()).thenReturn(false);

        HttpServerConfig config = HttpServerOptionsUtils.createHttpServerConfig(
                buildTimeConfig, httpConfig, LaunchMode.NORMAL, Collections.emptyList());

        assertThat(config).isNull();
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
        when(config.hostEnabled()).thenReturn(true);
        when(config.determinePort(Mockito.any())).thenReturn(8080);
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
        when(limits.maxHeaderSize()).thenReturn(MemorySize.of("20k"));
        when(limits.maxChunkSize()).thenReturn(MemorySize.of("8k"));
        when(limits.maxFormAttributeSize()).thenReturn(MemorySize.of("2k"));
        when(limits.maxFormFields()).thenReturn(256);
        when(limits.maxFormBufferedBytes()).thenReturn(MemorySize.of("1k"));
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

        when(config.tcpUserTimeout()).thenReturn(Duration.ZERO);
        when(config.soLinger()).thenReturn(-1);
        when(config.sendBufferSize()).thenReturn(OptionalInt.empty());
        when(config.receiveBufferSize()).thenReturn(OptionalInt.empty());
        when(config.readIdleTimeout()).thenReturn(Duration.ZERO);
        when(config.writeIdleTimeout()).thenReturn(Duration.ZERO);
        when(config.compressionContentSizeThreshold()).thenReturn(0);
        when(config.proxyProtocolTimeout()).thenReturn(Duration.ofSeconds(10));
        when(config.http2MaxSmallContinuationFrames()).thenReturn(OptionalInt.empty());
        when(config.decoderInitialBufferSize()).thenReturn(128);
        when(config.tcpKeepAlive()).thenReturn(false);
        when(config.logActivity()).thenReturn(false);
        when(config.activityLogDataFormat()).thenReturn(VertxHttpConfig.ActivityLogDataFormat.HEX_DUMP);
        when(config.reuseAddress()).thenReturn(true);
        when(config.trafficClass()).thenReturn(-1);

        WebsocketServerConfig ws = mock(WebsocketServerConfig.class);
        when(ws.maxFrameSize()).thenReturn(Optional.empty());
        when(ws.maxMessageSize()).thenReturn(Optional.empty());
        when(ws.perFrameCompression()).thenReturn(true);
        when(ws.perMessageCompression()).thenReturn(true);
        when(ws.compressionLevel()).thenReturn(6);
        when(ws.allowServerNoContext()).thenReturn(false);
        when(ws.preferredClientNoContext()).thenReturn(false);
        when(ws.closingTimeout()).thenReturn(10);
        when(ws.acceptUnmaskedFrames()).thenReturn(false);
        when(config.websocketServer()).thenReturn(ws);

        return config;
    }
}
