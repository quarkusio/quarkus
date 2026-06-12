package io.quarkus.vertx.http.runtime.options;

import static io.quarkus.vertx.http.runtime.options.HttpServerTlsConfig.getHttpServerTlsConfigName;
import static io.quarkus.vertx.http.runtime.options.HttpServerTlsConfig.getTlsClientAuth;
import static io.quarkus.vertx.http.runtime.options.TlsUtils.computeKeyStoreOptions;
import static io.quarkus.vertx.http.runtime.options.TlsUtils.computeTrustOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.vertx.http.runtime.ServerSslConfig;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig.InsecureRequests;
import io.quarkus.vertx.http.runtime.WebsocketServerConfig;
import io.quarkus.vertx.http.runtime.management.ManagementConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.CompressionConfig;
import io.vertx.core.http.FormDecoderConfig;
import io.vertx.core.http.Http1ServerConfig;
import io.vertx.core.http.Http2ServerConfig;
import io.vertx.core.http.Http2Settings;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.WebSocketServerConfig;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.core.net.TcpOption;
import io.vertx.core.net.TrafficShapingOptions;
import io.vertx.core.net.TrustOptions;

@SuppressWarnings("OptionalIsPresent")
public class HttpServerOptionsUtils {

    private static final Logger LOGGER = Logger.getLogger(HttpServerOptionsUtils.class);

    /**
     * When the http port is set to 0, replace it by this value to let Vert.x choose a random port
     */
    public static final int RANDOM_PORT_MAIN_HTTP = 10;

    /**
     * When the https port is set to 0, replace it by this value to let Vert.x choose a random port
     */
    public static final int RANDOM_PORT_MAIN_TLS = 20;

    /**
     * When the management port is set to 0, replace it by this value to let Vert.x choose a random port
     */
    public static final int RANDOM_PORT_MANAGEMENT = 30;

    /**
     * Holds the result of building server configuration: an {@link HttpServerConfig} and an optional
     * {@link ServerSSLOptions}.
     */
    public record ServerConfig(HttpServerConfig config, ServerSSLOptions sslOptions) {
    }

    /**
     * Create an {@link HttpServerConfig} and {@link ServerSSLOptions} for the HTTPS server,
     * or {@code null} if SSL should not be enabled.
     */
    public static ServerConfig createSslServerConfig(
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig,
            LaunchMode launchMode,
            List<String> websocketSubProtocols,
            TlsConfigurationRegistry registry) throws IOException {

        if (!httpConfig.hostEnabled()) {
            return null;
        }

        HttpServerConfig config = new HttpServerConfig();
        int sslPort = httpConfig.determineSslPort(launchMode);
        config.setPort(sslPort);

        Set<HttpVersion> versions = EnumSet.of(HttpVersion.HTTP_1_1);
        if (httpConfig.http2()) {
            versions.add(HttpVersion.HTTP_2);
        }
        config.setVersions(versions);

        setIdleTimeout(httpConfig, config);

        Optional<String> tlsConfigurationName = getHttpServerTlsConfigName(httpConfig, httpBuildTimeConfig, launchMode);
        TlsConfiguration bucket = getTlsConfiguration(tlsConfigurationName, registry);
        if (bucket != null) {
            ServerSSLOptions sslOptions = createSslOptionsFromTlsConfiguration(bucket);
            sslOptions.setClientAuth(getTlsClientAuth(httpConfig, httpBuildTimeConfig, launchMode));
            applyCommonOptions(config, httpBuildTimeConfig, httpConfig, websocketSubProtocols);
            return new ServerConfig(config, sslOptions);
        }

        // Legacy configuration
        ServerSSLOptions sslOptions = createSslOptionsFromLegacyConfig(httpConfig.ssl());
        if (sslOptions == null) {
            return null;
        }
        sslOptions.setClientAuth(getTlsClientAuth(httpConfig, httpBuildTimeConfig, launchMode));
        applyCommonOptions(config, httpBuildTimeConfig, httpConfig, websocketSubProtocols);
        return new ServerConfig(config, sslOptions);
    }

    /**
     * Create an {@link HttpServerConfig} and {@link ServerSSLOptions} for the management HTTPS server,
     * or {@code null} if SSL should not be enabled.
     */
    public static ServerConfig createSslServerConfigForManagementInterface(
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig,
            LaunchMode launchMode, List<String> websocketSubProtocols, TlsConfigurationRegistry registry)
            throws IOException {
        if (!managementConfig.hostEnabled()) {
            return null;
        }

        HttpServerConfig config = new HttpServerConfig();
        config.setVersions(EnumSet.of(HttpVersion.HTTP_1_1, HttpVersion.HTTP_2));
        config.setIdleTimeout(managementConfig.idleTimeout());

        int sslPort = managementConfig.determinePort(launchMode);
        config.setPort(sslPort);

        TlsConfiguration bucket = getTlsConfiguration(managementConfig.tlsConfigurationName(), registry);
        if (bucket != null) {
            ServerSSLOptions sslOptions = createSslOptionsFromTlsConfiguration(bucket);
            sslOptions.setClientAuth(managementBuildTimeConfig.tlsClientAuth());
            applyCommonOptionsForManagementInterface(config, managementBuildTimeConfig, managementConfig,
                    websocketSubProtocols);
            return new ServerConfig(config, sslOptions);
        }

        // Legacy configuration
        ServerSSLOptions sslOptions = createSslOptionsFromLegacyConfig(managementConfig.ssl());
        if (sslOptions == null) {
            return null;
        }
        sslOptions.setClientAuth(managementBuildTimeConfig.tlsClientAuth());
        applyCommonOptionsForManagementInterface(config, managementBuildTimeConfig, managementConfig,
                websocketSubProtocols);
        return new ServerConfig(config, sslOptions);
    }

    /**
     * Create {@link ServerSSLOptions} from a Quarkus TLS registry configuration.
     */
    public static ServerSSLOptions createSslOptionsFromTlsConfiguration(TlsConfiguration bucket) {
        ServerSSLOptions sslOptions = new ServerSSLOptions();

        KeyCertOptions keyStoreOptions = bucket.getKeyStoreOptions();
        TrustOptions trustStoreOptions = bucket.getTrustStoreOptions();
        if (keyStoreOptions != null) {
            sslOptions.setKeyCertOptions(keyStoreOptions);
        }
        if (trustStoreOptions != null) {
            sslOptions.setTrustOptions(trustStoreOptions);
        }
        sslOptions.setSni(bucket.usesSni());

        var other = bucket.getServerSSLOptions();
        sslOptions.setSslHandshakeTimeout(other.getSslHandshakeTimeout());
        sslOptions.setSslHandshakeTimeoutUnit(other.getSslHandshakeTimeoutUnit());
        for (String suite : other.getEnabledCipherSuites()) {
            sslOptions.addEnabledCipherSuite(suite);
        }
        for (Buffer buffer : other.getCrlValues()) {
            sslOptions.addCrlValue(buffer);
        }
        if (!other.isUseAlpn()) {
            sslOptions.setUseAlpn(false);
        }
        sslOptions.setEnabledSecureTransportProtocols(other.getEnabledSecureTransportProtocols());

        return sslOptions;
    }

    /**
     * Create an {@link HttpServerConfig} for the plain HTTP server.
     */
    public static HttpServerConfig createHttpServerConfig(
            VertxHttpBuildTimeConfig buildTimeConfig,
            VertxHttpConfig httpConfig,
            LaunchMode launchMode,
            List<String> websocketSubProtocols) {
        if (!httpConfig.hostEnabled()) {
            return null;
        }
        HttpServerConfig config = new HttpServerConfig();

        if (httpConfig.http2()) {
            config.setVersions(EnumSet.of(HttpVersion.HTTP_1_1, HttpVersion.HTTP_2));
        } else {
            config.setVersions(EnumSet.of(HttpVersion.HTTP_1_1));
        }

        int port = httpConfig.determinePort(launchMode);
        config.setPort(port);

        applyCommonOptions(config, buildTimeConfig, httpConfig, websocketSubProtocols);
        return config;
    }

    /**
     * Create an {@link HttpServerConfig} for the management plain HTTP server.
     */
    public static HttpServerConfig createHttpServerConfigForManagementInterface(
            ManagementInterfaceBuildTimeConfig buildTimeConfig,
            ManagementConfig httpConfig,
            LaunchMode launchMode,
            List<String> websocketSubProtocols) {
        if (!httpConfig.hostEnabled()) {
            return null;
        }
        HttpServerConfig config = new HttpServerConfig();
        int port = httpConfig.determinePort(launchMode);
        config.setPort(port);

        applyCommonOptionsForManagementInterface(config, buildTimeConfig, httpConfig, websocketSubProtocols);
        return config;
    }

    /**
     * Create an {@link HttpServerConfig} for a domain socket server, or {@code null} if not enabled.
     */
    public static HttpServerConfig createDomainSocketConfig(
            VertxHttpBuildTimeConfig buildTimeConfig,
            VertxHttpConfig httpConfig,
            List<String> websocketSubProtocols) {
        if (!httpConfig.domainSocketEnabled()) {
            return null;
        }
        HttpServerConfig config = new HttpServerConfig();
        applyCommonOptions(config, buildTimeConfig, httpConfig, websocketSubProtocols);
        config.setHost(httpConfig.domainSocket());
        return config;
    }

    /**
     * Create an {@link HttpServerConfig} for a management domain socket server, or {@code null} if not enabled.
     */
    public static HttpServerConfig createDomainSocketConfigForManagementInterface(
            ManagementInterfaceBuildTimeConfig buildTimeConfig,
            ManagementConfig managementConfig,
            List<String> websocketSubProtocols) {
        if (!managementConfig.domainSocketEnabled()) {
            return null;
        }
        HttpServerConfig config = new HttpServerConfig();
        applyCommonOptionsForManagementInterface(config, buildTimeConfig, managementConfig, websocketSubProtocols);
        config.setHost(managementConfig.domainSocket());
        return config;
    }

    /**
     * Apply common HTTP server options to an {@link HttpServerConfig}.
     */
    public static void applyCommonOptions(
            HttpServerConfig config,
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig,
            List<String> websocketSubProtocols) {
        config.setHost(httpConfig.host());
        setIdleTimeout(httpConfig, config);

        // HTTP/1.1 config
        Http1ServerConfig http1 = new Http1ServerConfig();
        http1.setMaxHeaderSize(httpConfig.limits().maxHeaderSize().asBigInteger().intValueExact());
        http1.setMaxChunkSize(httpConfig.limits().maxChunkSize().asBigInteger().intValueExact());
        http1.setMaxInitialLineLength(httpConfig.limits().maxInitialLineLength());
        config.setHttp1Config(http1);

        // Form decoder config
        FormDecoderConfig formConfig = new FormDecoderConfig();
        formConfig.setMaxAttributeSize(httpConfig.limits().maxFormAttributeSize().asBigInteger().intValueExact());
        formConfig.setMaxFields(httpConfig.limits().maxFormFields());
        formConfig.setMaxBufferedBytes(httpConfig.limits().maxFormBufferedBytes().asBigInteger().intValue());
        config.setFormDecoderConfig(formConfig);

        // WebSocket config
        WebSocketServerConfig wsConfig = new WebSocketServerConfig();
        wsConfig.setSubProtocols(websocketSubProtocols);
        httpConfig.websocketServer().maxFrameSize().ifPresent(wsConfig::setMaxFrameSize);
        httpConfig.websocketServer().maxMessageSize().ifPresent(wsConfig::setMaxMessageSize);
        applyWebSocketOptions(wsConfig, httpConfig.websocketServer());
        config.setWebSocketConfig(wsConfig);

        // TCP transport config
        var tcpConfig = config.getTcpConfig();
        var transport = tcpConfig.getTransportConfig();
        transport.setSoReusePort(httpConfig.soReusePort());
        transport.setOption(TcpOption.QUICKACK, httpConfig.tcpQuickAck());
        transport.setOption(TcpOption.CORK, httpConfig.tcpCork());
        tcpConfig.setAcceptBacklog(httpConfig.acceptBacklog());
        transport.setOption(TcpOption.FASTOPEN_CONNECT, httpConfig.tcpFastOpen());
        transport.setOption(TcpOption.USER_TIMEOUT, (int) httpConfig.tcpUserTimeout().toMillis());
        transport.setSoLinger(httpConfig.soLinger());
        if (httpConfig.sendBufferSize().isPresent()) {
            transport.setSendBufferSize(httpConfig.sendBufferSize().getAsInt());
        }
        if (httpConfig.receiveBufferSize().isPresent()) {
            transport.setReceiveBufferSize(httpConfig.receiveBufferSize().getAsInt());
        }
        config.setReadIdleTimeout(Duration.ofMillis(httpConfig.readIdleTimeout().toMillis()));
        config.setWriteIdleTimeout(Duration.ofMillis(httpConfig.writeIdleTimeout().toMillis()));

        config.setHandle100ContinueAutomatically(httpConfig.handle100ContinueAutomatically());

        // Compression config
        applyCompressionConfig(config, httpBuildTimeConfig);

        // HTTP/2 config
        if (httpConfig.http2()) {
            Http2ServerConfig http2 = new Http2ServerConfig();
            var settings = new Http2Settings();
            if (httpConfig.limits().headerTableSize().isPresent()) {
                settings.setHeaderTableSize(httpConfig.limits().headerTableSize().getAsLong());
            }
            settings.setPushEnabled(httpConfig.http2PushEnabled());
            if (httpConfig.limits().maxConcurrentStreams().isPresent()) {
                settings.setMaxConcurrentStreams(httpConfig.limits().maxConcurrentStreams().getAsLong());
            }
            if (httpConfig.initialWindowSize().isPresent()) {
                settings.setInitialWindowSize(httpConfig.initialWindowSize().getAsInt());
            }
            if (httpConfig.limits().maxFrameSize().isPresent()) {
                settings.setMaxFrameSize(httpConfig.limits().maxFrameSize().getAsInt());
            }
            if (httpConfig.limits().maxHeaderListSize().isPresent()) {
                settings.setMaxHeaderListSize(httpConfig.limits().maxHeaderListSize().getAsLong());
            }
            http2.setInitialSettings(settings);

            // RST attack protection
            if (httpConfig.limits().rstFloodMaxRstFramePerWindow().isPresent()) {
                http2.setRstFloodMaxRstFramePerWindow(httpConfig.limits().rstFloodMaxRstFramePerWindow().getAsInt());
            }
            if (httpConfig.limits().rstFloodWindowDuration().isPresent()) {
                http2.setRstFloodWindowDuration(httpConfig.limits().rstFloodWindowDuration().get());
            }
            if (httpConfig.http2ConnectionWindowSize().isPresent()) {
                http2.setConnectionWindowSize(httpConfig.http2ConnectionWindowSize().getAsInt());
            }
            config.setHttp2Config(http2);
        }

        // Proxy protocol
        config.getTcpConfig().setUseProxyProtocol(httpConfig.proxy().useProxyProtocol());

        // Traffic shaping
        configureTrafficShapingIfEnabled(config, httpConfig);
    }

    /**
     * Apply common management interface options to an {@link HttpServerConfig}.
     */
    public static void applyCommonOptionsForManagementInterface(
            HttpServerConfig config,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig,
            List<String> websocketSubProtocols) {
        config.setHost(managementConfig.host());
        config.setIdleTimeout(managementConfig.idleTimeout());

        // HTTP/1.1 config
        Http1ServerConfig http1 = new Http1ServerConfig();
        http1.setMaxHeaderSize(managementConfig.limits().maxHeaderSize().asBigInteger().intValueExact());
        http1.setMaxChunkSize(managementConfig.limits().maxChunkSize().asBigInteger().intValueExact());
        http1.setMaxInitialLineLength(managementConfig.limits().maxInitialLineLength());
        config.setHttp1Config(http1);

        // Form decoder config
        FormDecoderConfig formConfig = new FormDecoderConfig();
        formConfig.setMaxAttributeSize(managementConfig.limits().maxFormAttributeSize().asBigInteger().intValueExact());
        formConfig.setMaxFields(managementConfig.limits().maxFormFields());
        formConfig.setMaxBufferedBytes(managementConfig.limits().maxFormBufferedBytes().asBigInteger().intValue());
        config.setFormDecoderConfig(formConfig);

        // WebSocket config
        WebSocketServerConfig wsConfig = new WebSocketServerConfig();
        wsConfig.setSubProtocols(websocketSubProtocols);
        managementConfig.websocketServer().maxFrameSize().ifPresent(wsConfig::setMaxFrameSize);
        managementConfig.websocketServer().maxMessageSize().ifPresent(wsConfig::setMaxMessageSize);
        applyWebSocketOptions(wsConfig, managementConfig.websocketServer());
        config.setWebSocketConfig(wsConfig);

        config.getTcpConfig().setAcceptBacklog(managementConfig.acceptBacklog());

        config.setHandle100ContinueAutomatically(managementConfig.handle100ContinueAutomatically());

        // Compression
        applyCompressionConfig(config, managementBuildTimeConfig.enableCompression(),
                managementBuildTimeConfig.enableDecompression(), Optional.empty(),
                managementBuildTimeConfig.compressionLevel());

        // Proxy protocol
        config.getTcpConfig().setUseProxyProtocol(managementConfig.proxy().useProxyProtocol());

        // TCP options
        var transport = config.getTcpConfig().getTransportConfig();
        transport.setOption(TcpOption.USER_TIMEOUT, (int) managementConfig.tcpUserTimeout().toMillis());
        transport.setSoLinger(managementConfig.soLinger());
        if (managementConfig.sendBufferSize().isPresent()) {
            transport.setSendBufferSize(managementConfig.sendBufferSize().getAsInt());
        }
        if (managementConfig.receiveBufferSize().isPresent()) {
            transport.setReceiveBufferSize(managementConfig.receiveBufferSize().getAsInt());
        }
        config.setReadIdleTimeout(Duration.ofMillis(managementConfig.readIdleTimeout().toMillis()));
        config.setWriteIdleTimeout(Duration.ofMillis(managementConfig.writeIdleTimeout().toMillis()));
    }

    /**
     * @deprecated Use {@link #createSslServerConfig} instead.
     */
    @Deprecated(forRemoval = true)
    public static HttpServerOptions createSslOptions(
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig,
            LaunchMode launchMode,
            List<String> websocketSubProtocols,
            TlsConfigurationRegistry registry) throws IOException {

        if (!httpConfig.hostEnabled()) {
            return null;
        }

        final HttpServerOptions serverOptions = new HttpServerOptions();
        int sslPort = httpConfig.determineSslPort(launchMode);
        serverOptions.setPort(sslPort);
        serverOptions.setClientAuth(getTlsClientAuth(httpConfig, httpBuildTimeConfig, launchMode));

        if (JdkSSLEngineOptions.isAlpnAvailable()) {
            serverOptions.setUseAlpn(httpConfig.http2());
            if (httpConfig.http2()) {
                serverOptions.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
            }
        }
        setIdleTimeout(httpConfig, serverOptions);

        Optional<String> tlsConfigurationName = getHttpServerTlsConfigName(httpConfig, httpBuildTimeConfig, launchMode);
        TlsConfiguration bucket = getTlsConfiguration(tlsConfigurationName, registry);
        if (bucket != null) {
            applyTlsConfigurationToHttpServerOptions(bucket, serverOptions);
            applyCommonOptions(serverOptions, httpBuildTimeConfig, httpConfig, websocketSubProtocols);
            return serverOptions;
        }

        // Legacy configuration:
        applySslConfigToHttpServerOptions(httpConfig.ssl(), serverOptions);
        applyCommonOptions(serverOptions, httpBuildTimeConfig, httpConfig, websocketSubProtocols);

        return serverOptions;
    }

    /**
     * @deprecated Use {@link #createSslServerConfigForManagementInterface} instead.
     */
    @Deprecated(forRemoval = true)
    public static HttpServerOptions createSslOptionsForManagementInterface(
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig,
            LaunchMode launchMode, List<String> websocketSubProtocols, TlsConfigurationRegistry registry)
            throws IOException {
        if (!managementConfig.hostEnabled()) {
            return null;
        }

        final HttpServerOptions serverOptions = new HttpServerOptions();
        if (JdkSSLEngineOptions.isAlpnAvailable()) {
            serverOptions.setUseAlpn(true);
            serverOptions.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
        }
        int idleTimeout = (int) managementConfig.idleTimeout().toMillis();
        serverOptions.setIdleTimeout(idleTimeout);
        serverOptions.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);

        int sslPort = managementConfig.determinePort(launchMode);
        serverOptions.setPort(sslPort);
        serverOptions.setClientAuth(managementBuildTimeConfig.tlsClientAuth());

        TlsConfiguration bucket = getTlsConfiguration(managementConfig.tlsConfigurationName(), registry);
        if (bucket != null) {
            applyTlsConfigurationToHttpServerOptions(bucket, serverOptions);
            applyCommonOptionsForManagementInterface(serverOptions, managementBuildTimeConfig, managementConfig,
                    websocketSubProtocols);
            return serverOptions;
        }

        // Legacy configuration:
        applySslConfigToHttpServerOptions(managementConfig.ssl(), serverOptions);
        applyCommonOptionsForManagementInterface(serverOptions, managementBuildTimeConfig, managementConfig,
                websocketSubProtocols);

        return serverOptions;
    }

    /**
     * @deprecated Use {@link #createSslOptionsFromTlsConfiguration} instead.
     */
    @Deprecated(forRemoval = true)
    public static void applyTlsConfigurationToHttpServerOptions(TlsConfiguration bucket, HttpServerOptions serverOptions) {
        serverOptions.setSsl(true);

        KeyCertOptions keyStoreOptions = bucket.getKeyStoreOptions();
        TrustOptions trustStoreOptions = bucket.getTrustStoreOptions();
        if (keyStoreOptions != null) {
            serverOptions.setKeyCertOptions(keyStoreOptions);
        }
        if (trustStoreOptions != null) {
            serverOptions.setTrustOptions(trustStoreOptions);
        }
        serverOptions.setSni(bucket.usesSni());

        ServerSSLOptions other = bucket.getServerSSLOptions();
        serverOptions.setSslHandshakeTimeout(other.getSslHandshakeTimeout());
        serverOptions.setSslHandshakeTimeoutUnit(other.getSslHandshakeTimeoutUnit());
        for (String suite : other.getEnabledCipherSuites()) {
            serverOptions.addEnabledCipherSuite(suite);
        }
        for (Buffer buffer : other.getCrlValues()) {
            serverOptions.addCrlValue(buffer);
        }
        if (!other.isUseAlpn()) {
            serverOptions.setUseAlpn(false);
        }
        serverOptions.setEnabledSecureTransportProtocols(other.getEnabledSecureTransportProtocols());
    }

    /**
     * @deprecated Use {@link #applyCommonOptions(HttpServerConfig, VertxHttpBuildTimeConfig, VertxHttpConfig, List)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void applyCommonOptions(
            HttpServerOptions httpServerOptions,
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig,
            List<String> websocketSubProtocols) {
        httpServerOptions.setHost(httpConfig.host());
        setIdleTimeout(httpConfig, httpServerOptions);
        httpServerOptions.setMaxHeaderSize(httpConfig.limits().maxHeaderSize().asIntValue());
        httpServerOptions.setMaxChunkSize(httpConfig.limits().maxChunkSize().asIntValue());
        httpServerOptions.setMaxFormAttributeSize(httpConfig.limits().maxFormAttributeSize().asIntValue());
        httpServerOptions.setMaxFormFields(httpConfig.limits().maxFormFields());
        httpServerOptions.setMaxFormBufferedBytes(httpConfig.limits().maxFormBufferedBytes().asIntValue());
        httpServerOptions.setWebSocketSubProtocols(websocketSubProtocols);
        httpServerOptions.setReusePort(httpConfig.soReusePort());
        httpServerOptions.setTcpQuickAck(httpConfig.tcpQuickAck());
        httpServerOptions.setTcpCork(httpConfig.tcpCork());
        httpServerOptions.setAcceptBacklog(httpConfig.acceptBacklog());
        httpServerOptions.setTcpFastOpen(httpConfig.tcpFastOpen());
        httpServerOptions.setUseSemicolonAsQueryParamDelimiter(httpConfig.useSemicolonAsQueryParamDelimiter());
        httpServerOptions.setTcpUserTimeout((int) httpConfig.tcpUserTimeout().toMillis());
        httpServerOptions.setSoLinger(httpConfig.soLinger());
        if (httpConfig.sendBufferSize().isPresent()) {
            httpServerOptions.setSendBufferSize(httpConfig.sendBufferSize().getAsInt());
        }
        if (httpConfig.receiveBufferSize().isPresent()) {
            httpServerOptions.setReceiveBufferSize(httpConfig.receiveBufferSize().getAsInt());
        }
        httpServerOptions.setReadIdleTimeout((int) httpConfig.readIdleTimeout().toMillis());
        httpServerOptions.setWriteIdleTimeout((int) httpConfig.writeIdleTimeout().toMillis());
        httpServerOptions.setCompressionContentSizeThreshold(httpConfig.compressionContentSizeThreshold());
        httpServerOptions.setCompressionSupported(httpBuildTimeConfig.enableCompression());
        if (httpBuildTimeConfig.compressionLevel().isPresent()) {
            httpServerOptions.setCompressionLevel(httpBuildTimeConfig.compressionLevel().getAsInt());
        }
        httpServerOptions.setDecompressionSupported(httpBuildTimeConfig.enableDecompression());
        httpServerOptions.setMaxInitialLineLength(httpConfig.limits().maxInitialLineLength());
        httpServerOptions.setHandle100ContinueAutomatically(httpConfig.handle100ContinueAutomatically());

        if (httpBuildTimeConfig.compressors().isPresent()) {
            for (String compressor : httpBuildTimeConfig.compressors().get()) {
                if ("gzip".equalsIgnoreCase(compressor)) {
                    final GzipOptions defaultOps = StandardCompressionOptions.gzip();
                    httpServerOptions.addCompressor(StandardCompressionOptions
                            .gzip(httpServerOptions.getCompressionLevel(), defaultOps.windowBits(), defaultOps.memLevel()));
                } else if ("deflate".equalsIgnoreCase(compressor)) {
                    final DeflateOptions defaultOps = StandardCompressionOptions.deflate();
                    httpServerOptions.addCompressor(StandardCompressionOptions
                            .deflate(httpServerOptions.getCompressionLevel(), defaultOps.windowBits(), defaultOps.memLevel()));
                } else if ("br".equalsIgnoreCase(compressor)) {
                    final BrotliOptions o = StandardCompressionOptions.brotli();
                    if (httpBuildTimeConfig.compressionLevel().isPresent()) {
                        o.parameters().setQuality(httpBuildTimeConfig.compressionLevel().getAsInt());
                    }
                    httpServerOptions.addCompressor(o);
                } else {
                    LOGGER.errorf("Unknown compressor: %s", compressor);
                }
            }
        }

        if (httpConfig.http2()) {
            var settings = new Http2Settings();
            if (httpConfig.limits().headerTableSize().isPresent()) {
                settings.setHeaderTableSize(httpConfig.limits().headerTableSize().getAsLong());
            }
            settings.setPushEnabled(httpConfig.http2PushEnabled());
            if (httpConfig.limits().maxConcurrentStreams().isPresent()) {
                settings.setMaxConcurrentStreams(httpConfig.limits().maxConcurrentStreams().getAsLong());
            }
            if (httpConfig.initialWindowSize().isPresent()) {
                settings.setInitialWindowSize(httpConfig.initialWindowSize().getAsInt());
            }
            if (httpConfig.limits().maxFrameSize().isPresent()) {
                settings.setMaxFrameSize(httpConfig.limits().maxFrameSize().getAsInt());
            }
            if (httpConfig.limits().maxHeaderListSize().isPresent()) {
                settings.setMaxHeaderListSize(httpConfig.limits().maxHeaderListSize().getAsLong());
            }
            httpServerOptions.setInitialSettings(settings);

            // RST attack protection
            if (httpConfig.limits().rstFloodMaxRstFramePerWindow().isPresent()) {
                httpServerOptions
                        .setHttp2RstFloodMaxRstFramePerWindow(httpConfig.limits().rstFloodMaxRstFramePerWindow().getAsInt());
            }
            if (httpConfig.limits().rstFloodWindowDuration().isPresent()) {
                httpServerOptions.setHttp2RstFloodWindowDuration(
                        (int) httpConfig.limits().rstFloodWindowDuration().get().toSeconds());
                httpServerOptions.setHttp2RstFloodWindowDurationTimeUnit(TimeUnit.SECONDS);
            }
            if (httpConfig.http2ConnectionWindowSize().isPresent()) {
                httpServerOptions.setHttp2ConnectionWindowSize(httpConfig.http2ConnectionWindowSize().getAsInt());
            }
            if (httpConfig.http2MaxSmallContinuationFrames().isPresent()) {
                httpServerOptions
                        .setHttp2MaxSmallContinuationFrames(httpConfig.http2MaxSmallContinuationFrames().getAsInt());
            }
        } else {
            httpServerOptions.setHttp2ClearTextEnabled(false);
        }

        httpServerOptions.setUseProxyProtocol(httpConfig.proxy().useProxyProtocol());
        httpServerOptions.setProxyProtocolTimeout(httpConfig.proxyProtocolTimeout().toMillis());
        httpServerOptions.setProxyProtocolTimeoutUnit(TimeUnit.MILLISECONDS);
        httpServerOptions.setTcpKeepAlive(httpConfig.tcpKeepAlive());
        httpServerOptions.setLogActivity(httpConfig.logActivity());
        httpServerOptions.setActivityLogDataFormat(
                httpConfig.activityLogDataFormat() == VertxHttpConfig.ActivityLogDataFormat.SIMPLE
                        ? ByteBufFormat.SIMPLE
                        : ByteBufFormat.HEX_DUMP);
        httpServerOptions.setReuseAddress(httpConfig.reuseAddress());
        if (httpConfig.trafficClass() >= 0) {
            httpServerOptions.setTrafficClass(httpConfig.trafficClass());
        }
        httpServerOptions.setDecoderInitialBufferSize(httpConfig.decoderInitialBufferSize());
        configureTrafficShapingIfEnabled(httpServerOptions, httpConfig);

        // WebSocket options
        httpConfig.websocketServer().maxFrameSize().ifPresent(httpServerOptions::setMaxWebSocketFrameSize);
        httpConfig.websocketServer().maxMessageSize().ifPresent(httpServerOptions::setMaxWebSocketMessageSize);
        applyWebSocketOptions(httpServerOptions, httpConfig.websocketServer());
    }

    /**
     * @deprecated Use
     *             {@link #applyCommonOptionsForManagementInterface(HttpServerConfig, ManagementInterfaceBuildTimeConfig, ManagementConfig, List)}
     *             instead.
     */
    @Deprecated(forRemoval = true)
    public static void applyCommonOptionsForManagementInterface(
            HttpServerOptions options,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig,
            List<String> websocketSubProtocols) {
        options.setHost(managementConfig.host());

        int idleTimeout = (int) managementConfig.idleTimeout().toMillis();
        options.setIdleTimeout(idleTimeout);
        options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);

        options.setMaxHeaderSize(managementConfig.limits().maxHeaderSize().asIntValue());
        options.setMaxChunkSize(managementConfig.limits().maxChunkSize().asIntValue());
        options.setMaxFormAttributeSize(managementConfig.limits().maxFormAttributeSize().asIntValue());
        options.setMaxFormFields(managementConfig.limits().maxFormFields());
        options.setMaxFormBufferedBytes(managementConfig.limits().maxFormBufferedBytes().asIntValue());
        options.setMaxInitialLineLength(managementConfig.limits().maxInitialLineLength());
        options.setWebSocketSubProtocols(websocketSubProtocols);
        options.setAcceptBacklog(managementConfig.acceptBacklog());
        options.setCompressionSupported(managementBuildTimeConfig.enableCompression());
        if (managementBuildTimeConfig.compressionLevel().isPresent()) {
            options.setCompressionLevel(managementBuildTimeConfig.compressionLevel().getAsInt());
        } else {
            options.setCompressionLevel(HttpServerOptions.DEFAULT_COMPRESSION_LEVEL);
        }
        options.setDecompressionSupported(managementBuildTimeConfig.enableDecompression());
        options.setHandle100ContinueAutomatically(managementConfig.handle100ContinueAutomatically());
        options.setUseSemicolonAsQueryParamDelimiter(managementConfig.useSemicolonAsQueryParamDelimiter());

        options.setUseProxyProtocol(managementConfig.proxy().useProxyProtocol());
        options.setProxyProtocolTimeout(managementConfig.proxyProtocolTimeout().toMillis());
        options.setProxyProtocolTimeoutUnit(TimeUnit.MILLISECONDS);
        options.setTcpKeepAlive(managementConfig.tcpKeepAlive());
        options.setLogActivity(managementConfig.logActivity());
        options.setActivityLogDataFormat(
                managementConfig.activityLogDataFormat() == VertxHttpConfig.ActivityLogDataFormat.SIMPLE
                        ? ByteBufFormat.SIMPLE
                        : ByteBufFormat.HEX_DUMP);
        options.setReuseAddress(managementConfig.reuseAddress());
        if (managementConfig.trafficClass() >= 0) {
            options.setTrafficClass(managementConfig.trafficClass());
        }

        options.setTcpUserTimeout((int) managementConfig.tcpUserTimeout().toMillis());
        options.setSoLinger(managementConfig.soLinger());
        if (managementConfig.sendBufferSize().isPresent()) {
            options.setSendBufferSize(managementConfig.sendBufferSize().getAsInt());
        }
        if (managementConfig.receiveBufferSize().isPresent()) {
            options.setReceiveBufferSize(managementConfig.receiveBufferSize().getAsInt());
        }
        options.setReadIdleTimeout((int) managementConfig.readIdleTimeout().toMillis());
        options.setWriteIdleTimeout((int) managementConfig.writeIdleTimeout().toMillis());
        options.setCompressionContentSizeThreshold(managementConfig.compressionContentSizeThreshold());

        // WebSocket options
        managementConfig.websocketServer().maxFrameSize().ifPresent(options::setMaxWebSocketFrameSize);
        managementConfig.websocketServer().maxMessageSize().ifPresent(options::setMaxWebSocketMessageSize);
        applyWebSocketOptions(options, managementConfig.websocketServer());
    }

    public static Optional<String> getCredential(Optional<String> password, Map<String, String> credentials,
            Optional<String> passwordKey) {
        if (password.isPresent()) {
            return password;
        }

        if (passwordKey.isPresent()) {
            return Optional.ofNullable(credentials.get(passwordKey.get()));
        } else {
            return Optional.empty();
        }
    }

    public static InsecureRequests getInsecureRequestStrategy(VertxHttpConfig httpConfig,
            VertxHttpBuildTimeConfig httpBuildConfig, LaunchMode launchMode) {
        Optional<InsecureRequests> requests = httpConfig.insecureRequests();
        if (requests.isPresent()) {
            var value = requests.get();
            if (getTlsClientAuth(httpConfig, httpBuildConfig, launchMode) == ClientAuth.REQUIRED
                    && value == InsecureRequests.ENABLED) {
                Logger.getLogger(HttpServerOptionsUtils.class).warn(
                        "When configuring TLS client authentication to be required, it is recommended to **NOT** set `quarkus.http.insecure-requests` to `enabled`. "
                                +
                                "You can switch to `redirect` by setting `quarkus.http.insecure-requests=redirect`.");
            }
            return value;
        }
        if (getTlsClientAuth(httpConfig, httpBuildConfig, launchMode) == ClientAuth.REQUIRED) {
            Logger.getLogger(HttpServerOptionsUtils.class).info(
                    "TLS client authentication is required, thus disabling insecure requests. " +
                            "You can switch to `redirect` by setting `quarkus.http.insecure-requests=redirect`.");
            return InsecureRequests.DISABLED;
        }
        return InsecureRequests.ENABLED;
    }

    static byte[] getFileContent(Path path) throws IOException {
        byte[] data;
        final InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(ClassPathUtils.toResourceName(path));
        if (resource != null) {
            try (InputStream is = resource) {
                data = doRead(is);
            }
        } else {
            try (InputStream is = Files.newInputStream(path)) {
                data = doRead(is);
            }
        }
        return data;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Optional<T> or(Optional<T> a, Optional<T> b) {
        return a.isPresent() ? a : b;
    }

    private static TlsConfiguration getTlsConfiguration(Optional<String> tlsConfigurationName,
            TlsConfigurationRegistry registry) {
        TlsConfiguration bucket = null;
        if (tlsConfigurationName.isPresent()) {
            var maybeTlsConfig = registry.get(tlsConfigurationName.get());
            if (maybeTlsConfig.isEmpty()) {
                throw new ConfigurationException("No TLS configuration named " + tlsConfigurationName.get()
                        + " found in the TLS registry. Configure `quarkus.tls."
                        + tlsConfigurationName.get() + "` in your application.properties.");
            }
            bucket = maybeTlsConfig.get();
        } else if (registry != null && registry.getDefault().isPresent()
                && registry.getDefault().get().getKeyStoreOptions() != null) {
            bucket = registry.getDefault().get();
        }
        return bucket;
    }

    private static ServerSSLOptions createSslOptionsFromLegacyConfig(ServerSslConfig sslConfig) throws IOException {
        // credentials provider
        Map<String, String> credentials = Map.of();
        if (sslConfig.certificate().credentialsProvider().isPresent()) {
            String beanName = sslConfig.certificate().credentialsProviderName().orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = sslConfig.certificate().credentialsProvider().get();
            credentials = credentialsProvider.getCredentialsAsync(name).await().indefinitely();
        }

        final Optional<String> keyStorePassword = getCredential(sslConfig.certificate().keyStorePassword(), credentials,
                sslConfig.certificate().keyStorePasswordKey());

        Optional<String> keyStoreAliasPassword = Optional.empty();
        if (sslConfig.certificate().keyStoreAliasPassword().isPresent()
                || sslConfig.certificate().keyStoreKeyPassword().isPresent()
                || sslConfig.certificate().keyStoreKeyPasswordKey().isPresent()
                || sslConfig.certificate().keyStoreAliasPasswordKey().isPresent()) {
            if (sslConfig.certificate().keyStoreKeyPasswordKey().isPresent()
                    && sslConfig.certificate().keyStoreAliasPasswordKey().isPresent()) {
                throw new ConfigurationException(
                        "You cannot specify both `keyStoreKeyPasswordKey` and `keyStoreAliasPasswordKey` - Use `keyStoreAliasPasswordKey` instead");
            }
            if (sslConfig.certificate().keyStoreAliasPassword().isPresent()
                    && sslConfig.certificate().keyStoreKeyPassword().isPresent()) {
                throw new ConfigurationException(
                        "You cannot specify both `keyStoreKeyPassword` and `keyStoreAliasPassword` - Use `keyStoreAliasPassword` instead");
            }
            keyStoreAliasPassword = getCredential(
                    or(sslConfig.certificate().keyStoreAliasPassword(), sslConfig.certificate().keyStoreKeyPassword()),
                    credentials,
                    or(sslConfig.certificate().keyStoreAliasPasswordKey(), sslConfig.certificate().keyStoreKeyPasswordKey()));
        }

        final Optional<String> trustStorePassword = getCredential(sslConfig.certificate().trustStorePassword(), credentials,
                sslConfig.certificate().trustStorePasswordKey());

        ServerSSLOptions sslOptions = new ServerSSLOptions();
        var kso = computeKeyStoreOptions(sslConfig.certificate(), keyStorePassword, keyStoreAliasPassword);
        if (kso != null) {
            sslOptions.setKeyCertOptions(kso);
        }

        var to = computeTrustOptions(sslConfig.certificate(), trustStorePassword);
        if (to != null) {
            sslOptions.setTrustOptions(to);
        }

        for (String cipher : sslConfig.cipherSuites().orElse(Collections.emptyList())) {
            sslOptions.addEnabledCipherSuite(cipher);
        }

        sslOptions.setEnabledSecureTransportProtocols(sslConfig.protocols());
        sslOptions.setSni(sslConfig.sni());

        return sslOptions;
    }

    private static void applyCompressionConfig(HttpServerConfig config, VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        applyCompressionConfig(config, httpBuildTimeConfig.enableCompression(),
                httpBuildTimeConfig.enableDecompression(), httpBuildTimeConfig.compressors(),
                httpBuildTimeConfig.compressionLevel());
    }

    private static void applyCompressionConfig(HttpServerConfig config, boolean enableCompression,
            boolean enableDecompression, Optional<List<String>> compressors, OptionalInt compressionLevel) {
        CompressionConfig compression = new CompressionConfig();
        compression.setCompressionEnabled(enableCompression);
        compression.setDecompressionEnabled(enableDecompression);

        if (compressors.isPresent()) {
            for (String compressor : compressors.get()) {
                if ("gzip".equalsIgnoreCase(compressor)) {
                    if (compressionLevel.isPresent()) {
                        compression.addGzip(compressionLevel.getAsInt());
                    } else {
                        compression.addGzip();
                    }
                } else if ("deflate".equalsIgnoreCase(compressor)) {
                    if (compressionLevel.isPresent()) {
                        compression.addDeflate(compressionLevel.getAsInt());
                    } else {
                        compression.addDeflate();
                    }
                } else if ("br".equalsIgnoreCase(compressor)) {
                    if (compressionLevel.isPresent()) {
                        compression.addBrotli(compressionLevel.getAsInt());
                    } else {
                        compression.addBrotli();
                    }
                } else {
                    LOGGER.errorf("Unknown compressor: %s", compressor);
                }
            }
        } else {
            if (compressionLevel.isPresent()) {
                int level = compressionLevel.getAsInt();
                compression.addGzip(level);
                compression.addDeflate(level);
            } else {
                compression.addGzip();
                compression.addDeflate();
            }
        }

        config.setCompressionConfig(compression);
    }

    private static TrafficShapingOptions buildTrafficShapingOptions(VertxHttpConfig httpConfig) {
        TrafficShapingOptions options = new TrafficShapingOptions();
        if (httpConfig.trafficShaping().checkInterval().isPresent()) {
            options.setCheckIntervalForStats(httpConfig.trafficShaping().checkInterval().get().toSeconds());
            options.setCheckIntervalForStatsTimeUnit(TimeUnit.SECONDS);
        }
        if (httpConfig.trafficShaping().maxDelay().isPresent()) {
            options.setMaxDelayToWait(httpConfig.trafficShaping().maxDelay().get().toSeconds());
            options.setMaxDelayToWaitUnit(TimeUnit.SECONDS);
        }
        if (httpConfig.trafficShaping().inboundGlobalBandwidth().isPresent()) {
            options.setInboundGlobalBandwidth(httpConfig.trafficShaping().inboundGlobalBandwidth().get().asLongValue());
        }
        if (httpConfig.trafficShaping().outboundGlobalBandwidth().isPresent()) {
            options.setOutboundGlobalBandwidth(
                    httpConfig.trafficShaping().outboundGlobalBandwidth().get().asLongValue());
        }
        if (httpConfig.trafficShaping().peakOutboundGlobalBandwidth().isPresent()) {
            options.setPeakOutboundGlobalBandwidth(
                    httpConfig.trafficShaping().peakOutboundGlobalBandwidth().get().asLongValue());
        }
        return options;
    }

    private static void configureTrafficShapingIfEnabled(HttpServerConfig config, VertxHttpConfig httpConfig) {
        if (httpConfig.trafficShaping().enabled()) {
            config.getTcpConfig().setTrafficShapingOptions(buildTrafficShapingOptions(httpConfig));
        }
    }

    private static void applyWebSocketOptions(WebSocketServerConfig wsConfig, WebsocketServerConfig ws) {
        wsConfig.setUsePerFrameCompression(ws.perFrameCompression());
        wsConfig.setUsePerMessageCompression(ws.perMessageCompression());
        wsConfig.setCompressionLevel(ws.compressionLevel());
        wsConfig.setUseServerNoContext(ws.allowServerNoContext());
        wsConfig.setUseClientNoContext(ws.preferredClientNoContext());
        wsConfig.setClosingTimeout(Duration.ofSeconds(ws.closingTimeout()));
        wsConfig.setUseUnmaskedFrames(ws.acceptUnmaskedFrames());
    }

    private static void setIdleTimeout(VertxHttpConfig httpConfig, HttpServerConfig config) {
        config.setIdleTimeout(httpConfig.idleTimeout());
    }

    private static byte[] doRead(InputStream is) throws IOException {
        return is.readAllBytes();
    }
}
