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

import io.netty.handler.logging.ByteBufFormat;
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
import io.vertx.core.http.QueryParamDecoderConfig;
import io.vertx.core.http.WebSocketServerConfig;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.LogConfig;
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
            applyCommonOptions(config, httpBuildTimeConfig, httpConfig, websocketSubProtocols,
                    httpConfig.determineSslHost());
            return new ServerConfig(config, sslOptions);
        }

        // Legacy configuration
        ServerSSLOptions sslOptions = createSslOptionsFromLegacyConfig(httpConfig.ssl());
        if (sslOptions == null) {
            return null;
        }
        sslOptions.setClientAuth(getTlsClientAuth(httpConfig, httpBuildTimeConfig, launchMode));
        applyCommonOptions(config, httpBuildTimeConfig, httpConfig, websocketSubProtocols,
                httpConfig.determineSslHost());
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

        applyCommonOptions(config, buildTimeConfig, httpConfig, websocketSubProtocols, httpConfig.host());
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
        applyCommonOptions(config, buildTimeConfig, httpConfig, websocketSubProtocols, httpConfig.host());
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
            List<String> websocketSubProtocols,
            String host) {
        config.setHost(host);
        setIdleTimeout(httpConfig, config);

        // HTTP/1.1 config
        Http1ServerConfig http1 = new Http1ServerConfig();
        http1.setMaxHeaderSize(httpConfig.limits().maxHeaderSize().asBigInteger().intValueExact());
        http1.setMaxChunkSize(httpConfig.limits().maxChunkSize().asBigInteger().intValueExact());
        http1.setMaxInitialLineLength(httpConfig.limits().maxInitialLineLength());
        http1.setDecoderInitialBufferSize(httpConfig.decoderInitialBufferSize());
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
        transport.setSoKeepAlive(httpConfig.tcpKeepAlive());
        transport.setReuseAddress(httpConfig.reuseAddress());
        if (httpConfig.trafficClass() >= 0) {
            transport.setTrafficClass(httpConfig.trafficClass());
        }
        if (httpConfig.sendBufferSize().isPresent()) {
            transport.setSendBufferSize(httpConfig.sendBufferSize().getAsInt());
        }
        if (httpConfig.receiveBufferSize().isPresent()) {
            transport.setReceiveBufferSize(httpConfig.receiveBufferSize().getAsInt());
        }
        tcpConfig.setProxyProtocolTimeout(httpConfig.proxyProtocolTimeout());
        config.setReadIdleTimeout(Duration.ofMillis(httpConfig.readIdleTimeout().toMillis()));
        config.setWriteIdleTimeout(Duration.ofMillis(httpConfig.writeIdleTimeout().toMillis()));

        config.setHandle100ContinueAutomatically(httpConfig.handle100ContinueAutomatically());

        // Query param decoder config
        config.setQueryParamConfig(new QueryParamDecoderConfig()
                .setUseSemicolonAsDelimiter(httpConfig.useSemicolonAsQueryParamDelimiter()));

        // Compression config
        applyCompressionConfig(config, httpBuildTimeConfig, httpConfig.compressionContentSizeThreshold());

        // Logging
        if (httpConfig.logActivity()) {
            var log = new LogConfig();
            if (httpConfig.activityLogDataFormat() != null) {
                log.setDataFormat(ByteBufFormat.valueOf(httpConfig.activityLogDataFormat().name()));
            }
            config.setLogConfig(log);
        }

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
            if (httpConfig.http2MaxSmallContinuationFrames().isPresent()) {
                http2.setMaxSmallContinuationFrames(httpConfig.http2MaxSmallContinuationFrames().getAsInt());
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

        var tcpConfig = config.getTcpConfig();
        tcpConfig.setAcceptBacklog(managementConfig.acceptBacklog());

        config.setHandle100ContinueAutomatically(managementConfig.handle100ContinueAutomatically());

        // Query param decoder config
        config.setQueryParamConfig(new QueryParamDecoderConfig()
                .setUseSemicolonAsDelimiter(managementConfig.useSemicolonAsQueryParamDelimiter()));

        // Compression
        applyCompressionConfig(config, managementBuildTimeConfig.enableCompression(),
                managementBuildTimeConfig.enableDecompression(), Optional.empty(),
                managementBuildTimeConfig.compressionLevel(), managementConfig.compressionContentSizeThreshold());

        // Logging
        if (managementConfig.logActivity()) {
            var log = new LogConfig();
            if (managementConfig.activityLogDataFormat() != null) {
                log.setDataFormat(ByteBufFormat.valueOf(managementConfig.activityLogDataFormat().name()));
            }
            config.setLogConfig(log);
        }

        // Proxy protocol
        tcpConfig.setUseProxyProtocol(managementConfig.proxy().useProxyProtocol());
        tcpConfig.setProxyProtocolTimeout(managementConfig.proxyProtocolTimeout());

        // TCP options
        var transport = tcpConfig.getTransportConfig();
        transport.setOption(TcpOption.USER_TIMEOUT, (int) managementConfig.tcpUserTimeout().toMillis());
        transport.setSoLinger(managementConfig.soLinger());
        transport.setSoKeepAlive(managementConfig.tcpKeepAlive());
        transport.setReuseAddress(managementConfig.reuseAddress());
        if (managementConfig.trafficClass() >= 0) {
            transport.setTrafficClass(managementConfig.trafficClass());
        }
        if (managementConfig.sendBufferSize().isPresent()) {
            transport.setSendBufferSize(managementConfig.sendBufferSize().getAsInt());
        }
        if (managementConfig.receiveBufferSize().isPresent()) {
            transport.setReceiveBufferSize(managementConfig.receiveBufferSize().getAsInt());
        }
        config.setReadIdleTimeout(Duration.ofMillis(managementConfig.readIdleTimeout().toMillis()));
        config.setWriteIdleTimeout(Duration.ofMillis(managementConfig.writeIdleTimeout().toMillis()));
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

    private static void applyCompressionConfig(HttpServerConfig config, VertxHttpBuildTimeConfig httpBuildTimeConfig,
            int contentSizeThreshold) {
        applyCompressionConfig(config, httpBuildTimeConfig.enableCompression(),
                httpBuildTimeConfig.enableDecompression(), httpBuildTimeConfig.compressors(),
                httpBuildTimeConfig.compressionLevel(), contentSizeThreshold);
    }

    private static void applyCompressionConfig(HttpServerConfig config, boolean enableCompression,
            boolean enableDecompression, Optional<List<String>> compressors, OptionalInt compressionLevel,
            int contentSizeThreshold) {
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
                    // For now, do not configure the quality level for Brotli - See https://github.com/eclipse-vertx/vert.x/issues/6201
                    //                    if (compressionLevel.isPresent()) {
                    //                        compression.addBrotli(compressionLevel.getAsInt());
                    //                    } else {
                    compression.addBrotli();
                    //                    }
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

        compression.setContentSizeThreshold(contentSizeThreshold);
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
