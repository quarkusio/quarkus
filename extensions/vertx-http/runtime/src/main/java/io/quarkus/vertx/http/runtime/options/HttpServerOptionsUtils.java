package io.quarkus.vertx.http.runtime.options;

import static io.quarkus.vertx.http.runtime.options.TlsUtils.computeKeyStoreOptions;
import static io.quarkus.vertx.http.runtime.options.TlsUtils.computeTrustOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.netty.handler.codec.compression.BrotliOptions;
import io.netty.handler.codec.compression.DeflateOptions;
import io.netty.handler.codec.compression.GzipOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;
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
import io.quarkus.vertx.http.runtime.management.ManagementConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.Http2Settings;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TCPSSLOptions;
import io.vertx.core.net.TrafficShapingOptions;
import io.vertx.core.net.TrustOptions;

@SuppressWarnings("OptionalIsPresent")
public class HttpServerOptionsUtils {

    private static final boolean JDK_SSL_BUFFER_POOLING = Boolean.getBoolean("quarkus.http.server.ssl.jdk.bufferPooling");

    /**
     * When the http port is set to 0, replace it by this value to let Vert.x choose a random port
     */
    public static final int RANDOM_PORT_MAIN_HTTP = -1;

    /**
     * When the https port is set to 0, replace it by this value to let Vert.x choose a random port
     */
    public static final int RANDOM_PORT_MAIN_TLS = -2;

    /**
     * When the management port is set to 0, replace it by this value to let Vert.x choose a random port
     */
    public static final int RANDOM_PORT_MANAGEMENT = -3;

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     */
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
        // -2 instead of -1 (see http) to have vert.x assign two different random ports if both http and https shall be random
        serverOptions.setPort(sslPort == 0 ? RANDOM_PORT_MAIN_TLS : sslPort);
        serverOptions.setClientAuth(httpBuildTimeConfig.tlsClientAuth());

        if (JdkSSLEngineOptions.isAlpnAvailable()) {
            serverOptions.setUseAlpn(httpConfig.http2());
            if (httpConfig.http2()) {
                serverOptions.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
            }
        }
        setIdleTimeout(httpConfig, serverOptions);

        TlsConfiguration bucket = getTlsConfiguration(httpConfig.tlsConfigurationName(), registry);
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
            // Verify that default is present and a key store has been configured, otherwise we get the default configuration.
            bucket = registry.getDefault().get();
        }
        return bucket;
    }

    private static void applySslConfigToHttpServerOptions(ServerSslConfig sslConfig, HttpServerOptions serverOptions)
            throws IOException {
        // credentials provider
        Map<String, String> credentials = Map.of();
        if (sslConfig.certificate().credentialsProvider().isPresent()) {
            String beanName = sslConfig.certificate().credentialsProviderName().orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = sslConfig.certificate().credentialsProvider().get();
            credentials = credentialsProvider.getCredentials(name);
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

        var kso = computeKeyStoreOptions(sslConfig.certificate(), keyStorePassword, keyStoreAliasPassword);
        if (kso != null) {
            serverOptions.setKeyCertOptions(kso);
        }

        var to = computeTrustOptions(sslConfig.certificate(), trustStorePassword);
        if (to != null) {
            serverOptions.setTrustOptions(to);
        }

        for (String cipher : sslConfig.cipherSuites().orElse(Collections.emptyList())) {
            serverOptions.addEnabledCipherSuite(cipher);
        }

        serverOptions.setEnabledSecureTransportProtocols(sslConfig.protocols());
        serverOptions.setSsl(true);
        serverOptions.setSni(sslConfig.sni());
        setJdkHeapBufferPooling(serverOptions);
    }

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     */
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
        serverOptions.setPort(sslPort == 0 ? RANDOM_PORT_MANAGEMENT : sslPort);
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

    public static void applyTlsConfigurationToHttpServerOptions(TlsConfiguration bucket, HttpServerOptions serverOptions) {
        serverOptions.setSsl(true);
        setJdkHeapBufferPooling(serverOptions);

        KeyCertOptions keyStoreOptions = bucket.getKeyStoreOptions();
        TrustOptions trustStoreOptions = bucket.getTrustStoreOptions();
        if (keyStoreOptions != null) {
            serverOptions.setKeyCertOptions(keyStoreOptions);
        }
        if (trustStoreOptions != null) {
            serverOptions.setTrustOptions(trustStoreOptions);
        }
        serverOptions.setSni(bucket.usesSni());

        var other = bucket.getSSLOptions();
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

    private static void setJdkHeapBufferPooling(TCPSSLOptions tcpSslOptions) {
        if (!JDK_SSL_BUFFER_POOLING) {
            return;
        }
        var engineOption = tcpSslOptions.getSslEngineOptions();
        if (engineOption == null) {
            var jdkEngineOptions = new JdkSSLEngineOptions();
            jdkEngineOptions.setPooledHeapBuffers(true);
            tcpSslOptions.setSslEngineOptions(jdkEngineOptions);
        } else if (engineOption instanceof JdkSSLEngineOptions jdkEngineOptions) {
            jdkEngineOptions.setPooledHeapBuffers(true);
        }
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

    public static void applyCommonOptions(
            HttpServerOptions httpServerOptions,
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig,
            List<String> websocketSubProtocols) {
        httpServerOptions.setHost(httpConfig.host());
        setIdleTimeout(httpConfig, httpServerOptions);
        httpServerOptions.setMaxHeaderSize(httpConfig.limits().maxHeaderSize().asBigInteger().intValueExact());
        httpServerOptions.setMaxChunkSize(httpConfig.limits().maxChunkSize().asBigInteger().intValueExact());
        httpServerOptions.setMaxFormAttributeSize(httpConfig.limits().maxFormAttributeSize().asBigInteger().intValueExact());
        httpServerOptions.setMaxFormFields(httpConfig.limits().maxFormFields());
        httpServerOptions.setMaxFormBufferedBytes(httpConfig.limits().maxFormBufferedBytes().asBigInteger().intValue());
        httpServerOptions.setWebSocketSubProtocols(websocketSubProtocols);
        httpServerOptions.setReusePort(httpConfig.soReusePort());
        httpServerOptions.setTcpQuickAck(httpConfig.tcpQuickAck());
        httpServerOptions.setTcpCork(httpConfig.tcpCork());
        httpServerOptions.setAcceptBacklog(httpConfig.acceptBacklog());
        httpServerOptions.setTcpFastOpen(httpConfig.tcpFastOpen());
        httpServerOptions.setCompressionSupported(httpBuildTimeConfig.enableCompression());
        if (httpBuildTimeConfig.compressionLevel().isPresent()) {
            httpServerOptions.setCompressionLevel(httpBuildTimeConfig.compressionLevel().getAsInt());
        }
        httpServerOptions.setDecompressionSupported(httpBuildTimeConfig.enableDecompression());
        httpServerOptions.setMaxInitialLineLength(httpConfig.limits().maxInitialLineLength());
        httpServerOptions.setHandle100ContinueAutomatically(httpConfig.handle100ContinueAutomatically());

        if (httpBuildTimeConfig.compressors().isPresent()) {
            // Adding defaults too, because mere addition of .addCompressor(brotli) actually
            // overrides the default deflate and gzip capability.
            for (String compressor : httpBuildTimeConfig.compressors().get()) {
                if ("gzip".equalsIgnoreCase(compressor)) {
                    // GZip's default compression level is 6 in Netty Codec 4.1, the same
                    // as the default compression level in Vert.x Core 4.5.7's HttpServerOptions.
                    final GzipOptions defaultOps = StandardCompressionOptions.gzip();
                    httpServerOptions.addCompressor(StandardCompressionOptions
                            .gzip(httpServerOptions.getCompressionLevel(), defaultOps.windowBits(), defaultOps.memLevel()));
                } else if ("deflate".equalsIgnoreCase(compressor)) {
                    // Deflate's default compression level defaults the same as with GZip.
                    final DeflateOptions defaultOps = StandardCompressionOptions.deflate();
                    httpServerOptions.addCompressor(StandardCompressionOptions
                            .deflate(httpServerOptions.getCompressionLevel(), defaultOps.windowBits(), defaultOps.memLevel()));
                } else if ("br".equalsIgnoreCase(compressor)) {
                    final BrotliOptions o = StandardCompressionOptions.brotli();
                    // The default compression level for brotli as of Netty Codec 4.1 is 4,
                    // so we don't pick up Vert.x Core 4.5.7's default of 6. User can override:
                    if (httpBuildTimeConfig.compressionLevel().isPresent()) {
                        o.parameters().setQuality(httpBuildTimeConfig.compressionLevel().getAsInt());
                    }
                    httpServerOptions.addCompressor(o);
                } else {
                    Logger.getLogger(HttpServerOptionsUtils.class).errorf("Unknown compressor: %s", compressor);
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

            // RST attack protection - https://github.com/netty/netty/security/advisories/GHSA-xpw8-rcwv-8f8p
            if (httpConfig.limits().rstFloodMaxRstFramePerWindow().isPresent()) {
                httpServerOptions
                        .setHttp2RstFloodMaxRstFramePerWindow(httpConfig.limits().rstFloodMaxRstFramePerWindow().getAsInt());
            }
            if (httpConfig.limits().rstFloodWindowDuration().isPresent()) {
                httpServerOptions.setHttp2RstFloodWindowDuration(
                        (int) httpConfig.limits().rstFloodWindowDuration().get().toSeconds());
                httpServerOptions.setHttp2RstFloodWindowDurationTimeUnit(TimeUnit.SECONDS);
            }

        }

        httpServerOptions.setUseProxyProtocol(httpConfig.proxy().useProxyProtocol());
        configureTrafficShapingIfEnabled(httpServerOptions, httpConfig);
    }

    private static void configureTrafficShapingIfEnabled(HttpServerOptions httpServerOptions,
            VertxHttpConfig httpConfig) {
        if (httpConfig.trafficShaping().enabled()) {
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
            httpServerOptions.setTrafficShapingOptions(options);
        }
    }

    public static void applyCommonOptionsForManagementInterface(
            HttpServerOptions options,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig,
            List<String> websocketSubProtocols) {
        options.setHost(managementConfig.host());

        int idleTimeout = (int) managementConfig.idleTimeout().toMillis();
        options.setIdleTimeout(idleTimeout);
        options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);

        options.setMaxHeaderSize(managementConfig.limits().maxHeaderSize().asBigInteger().intValueExact());
        options.setMaxChunkSize(managementConfig.limits().maxChunkSize().asBigInteger().intValueExact());
        options.setMaxFormAttributeSize(managementConfig.limits().maxFormAttributeSize().asBigInteger().intValueExact());
        options.setMaxFormFields(managementConfig.limits().maxFormFields());
        options.setMaxFormBufferedBytes(managementConfig.limits().maxFormBufferedBytes().asBigInteger().intValue());
        options.setMaxInitialLineLength(managementConfig.limits().maxInitialLineLength());
        options.setWebSocketSubProtocols(websocketSubProtocols);
        options.setAcceptBacklog(managementConfig.acceptBacklog());
        options.setCompressionSupported(managementBuildTimeConfig.enableCompression());
        if (managementBuildTimeConfig.compressionLevel().isPresent()) {
            options.setCompressionLevel(managementBuildTimeConfig.compressionLevel().getAsInt());
        }
        options.setDecompressionSupported(managementBuildTimeConfig.enableDecompression());
        options.setHandle100ContinueAutomatically(managementConfig.handle100ContinueAutomatically());

        options.setUseProxyProtocol(managementConfig.proxy().useProxyProtocol());
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

    private static byte[] doRead(InputStream is) throws IOException {
        return is.readAllBytes();
    }

    private static void setIdleTimeout(VertxHttpConfig httpConfig, HttpServerOptions options) {
        int idleTimeout = (int) httpConfig.idleTimeout().toMillis();
        options.setIdleTimeout(idleTimeout);
        options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
    }

    public static InsecureRequests getInsecureRequestStrategy(VertxHttpBuildTimeConfig httpBuildTimeConfig,
            Optional<InsecureRequests> requests) {
        if (requests.isPresent()) {
            var value = requests.get();
            if (httpBuildTimeConfig.tlsClientAuth() == ClientAuth.REQUIRED && value == InsecureRequests.ENABLED) {
                Logger.getLogger(HttpServerOptionsUtils.class).warn(
                        "When configuring TLS client authentication to be required, it is recommended to **NOT** set `quarkus.http.insecure-requests` to `enabled`. "
                                +
                                "You can switch to `redirect` by setting `quarkus.http.insecure-requests=redirect`.");
            }
            return value;
        }
        if (httpBuildTimeConfig.tlsClientAuth() == ClientAuth.REQUIRED) {
            Logger.getLogger(HttpServerOptionsUtils.class).info(
                    "TLS client authentication is required, thus disabling insecure requests. " +
                            "You can switch to `redirect` by setting `quarkus.http.insecure-requests=redirect`.");
            return InsecureRequests.DISABLED;
        }
        return InsecureRequests.ENABLED;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Optional<T> or(Optional<T> a, Optional<T> b) {
        return a.isPresent() ? a : b;
    }
}
