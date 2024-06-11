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
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.ServerSslConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceConfiguration;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.Http2Settings;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrafficShapingOptions;
import io.vertx.core.net.TrustOptions;

@SuppressWarnings("OptionalIsPresent")
public class HttpServerOptionsUtils {

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
    public static HttpServerOptions createSslOptions(HttpBuildTimeConfig buildTimeConfig, HttpConfiguration httpConfiguration,
            LaunchMode launchMode, List<String> websocketSubProtocols, TlsConfigurationRegistry registry)
            throws IOException {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }

        final HttpServerOptions serverOptions = new HttpServerOptions();
        int sslPort = httpConfiguration.determineSslPort(launchMode);
        // -2 instead of -1 (see http) to have vert.x assign two different random ports if both http and https shall be random
        serverOptions.setPort(sslPort == 0 ? RANDOM_PORT_MAIN_TLS : sslPort);
        serverOptions.setClientAuth(buildTimeConfig.tlsClientAuth);

        if (JdkSSLEngineOptions.isAlpnAvailable()) {
            serverOptions.setUseAlpn(httpConfiguration.http2);
            if (httpConfiguration.http2) {
                serverOptions.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
            }
        }
        setIdleTimeout(httpConfiguration, serverOptions);

        TlsConfiguration bucket = getTlsConfiguration(httpConfiguration.tlsConfigurationName, registry);
        if (bucket != null) {
            applyTlsConfigurationToHttpServerOptions(bucket, serverOptions);
            applyCommonOptions(serverOptions, buildTimeConfig, httpConfiguration, websocketSubProtocols);
            return serverOptions;
        }

        // Legacy configuration:
        applySslConfigToHttpServerOptions(httpConfiguration.ssl, serverOptions);
        applyCommonOptions(serverOptions, buildTimeConfig, httpConfiguration, websocketSubProtocols);

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

    private static void applySslConfigToHttpServerOptions(ServerSslConfig httpConfiguration, HttpServerOptions serverOptions)
            throws IOException {
        ServerSslConfig sslConfig = httpConfiguration;
        // credentials provider
        Map<String, String> credentials = Map.of();
        if (sslConfig.certificate.credentialsProvider.isPresent()) {
            String beanName = sslConfig.certificate.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = sslConfig.certificate.credentialsProvider.get();
            credentials = credentialsProvider.getCredentials(name);
        }

        final Optional<String> keyStorePassword = getCredential(sslConfig.certificate.keyStorePassword, credentials,
                sslConfig.certificate.keyStorePasswordKey);

        Optional<String> keyStoreAliasPassword = Optional.empty();
        if (sslConfig.certificate.keyStoreAliasPassword.isPresent() || sslConfig.certificate.keyStoreKeyPassword.isPresent()
                || sslConfig.certificate.keyStoreKeyPasswordKey.isPresent()
                || sslConfig.certificate.keyStoreAliasPasswordKey.isPresent()) {
            if (sslConfig.certificate.keyStoreKeyPasswordKey.isPresent()
                    && sslConfig.certificate.keyStoreAliasPasswordKey.isPresent()) {
                throw new ConfigurationException(
                        "You cannot specify both `keyStoreKeyPasswordKey` and `keyStoreAliasPasswordKey` - Use `keyStoreAliasPasswordKey` instead");
            }
            if (sslConfig.certificate.keyStoreAliasPassword.isPresent()
                    && sslConfig.certificate.keyStoreKeyPassword.isPresent()) {
                throw new ConfigurationException(
                        "You cannot specify both `keyStoreKeyPassword` and `keyStoreAliasPassword` - Use `keyStoreAliasPassword` instead");
            }
            keyStoreAliasPassword = getCredential(
                    or(sslConfig.certificate.keyStoreAliasPassword, sslConfig.certificate.keyStoreKeyPassword),
                    credentials,
                    or(sslConfig.certificate.keyStoreAliasPasswordKey, sslConfig.certificate.keyStoreKeyPasswordKey));
        }

        final Optional<String> trustStorePassword = getCredential(sslConfig.certificate.trustStorePassword, credentials,
                sslConfig.certificate.trustStorePasswordKey);

        var kso = computeKeyStoreOptions(sslConfig.certificate, keyStorePassword, keyStoreAliasPassword);
        if (kso != null) {
            serverOptions.setKeyCertOptions(kso);
        }

        var to = computeTrustOptions(sslConfig.certificate, trustStorePassword);
        if (to != null) {
            serverOptions.setTrustOptions(to);
        }

        for (String cipher : sslConfig.cipherSuites.orElse(Collections.emptyList())) {
            serverOptions.addEnabledCipherSuite(cipher);
        }

        serverOptions.setEnabledSecureTransportProtocols(sslConfig.protocols);
        serverOptions.setSsl(true);
        serverOptions.setSni(sslConfig.sni);
    }

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     */
    public static HttpServerOptions createSslOptionsForManagementInterface(ManagementInterfaceBuildTimeConfig buildTimeConfig,
            ManagementInterfaceConfiguration httpConfiguration,
            LaunchMode launchMode, List<String> websocketSubProtocols, TlsConfigurationRegistry registry)
            throws IOException {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }

        final HttpServerOptions serverOptions = new HttpServerOptions();
        if (JdkSSLEngineOptions.isAlpnAvailable()) {
            serverOptions.setUseAlpn(true);
            serverOptions.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
        }
        int idleTimeout = (int) httpConfiguration.idleTimeout.toMillis();
        serverOptions.setIdleTimeout(idleTimeout);
        serverOptions.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);

        int sslPort = httpConfiguration.determinePort(launchMode);
        serverOptions.setPort(sslPort == 0 ? RANDOM_PORT_MANAGEMENT : sslPort);
        serverOptions.setClientAuth(buildTimeConfig.tlsClientAuth);

        TlsConfiguration bucket = getTlsConfiguration(httpConfiguration.tlsConfigurationName, registry);
        if (bucket != null) {
            applyTlsConfigurationToHttpServerOptions(bucket, serverOptions);
            applyCommonOptionsForManagementInterface(serverOptions, buildTimeConfig, httpConfiguration, websocketSubProtocols);
            return serverOptions;
        }

        // Legacy configuration:
        applySslConfigToHttpServerOptions(httpConfiguration.ssl, serverOptions);
        applyCommonOptionsForManagementInterface(serverOptions, buildTimeConfig, httpConfiguration, websocketSubProtocols);

        return serverOptions;
    }

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

    public static void applyCommonOptions(HttpServerOptions httpServerOptions,
            HttpBuildTimeConfig buildTimeConfig,
            HttpConfiguration httpConfiguration,
            List<String> websocketSubProtocols) {
        httpServerOptions.setHost(httpConfiguration.host);
        setIdleTimeout(httpConfiguration, httpServerOptions);
        httpServerOptions.setMaxHeaderSize(httpConfiguration.limits.maxHeaderSize.asBigInteger().intValueExact());
        httpServerOptions.setMaxChunkSize(httpConfiguration.limits.maxChunkSize.asBigInteger().intValueExact());
        httpServerOptions.setMaxFormAttributeSize(httpConfiguration.limits.maxFormAttributeSize.asBigInteger().intValueExact());
        httpServerOptions.setMaxFormFields(httpConfiguration.limits.maxFormFields);
        httpServerOptions.setMaxFormBufferedBytes(httpConfiguration.limits.maxFormBufferedBytes.asBigInteger().intValue());
        httpServerOptions.setWebSocketSubProtocols(websocketSubProtocols);
        httpServerOptions.setReusePort(httpConfiguration.soReusePort);
        httpServerOptions.setTcpQuickAck(httpConfiguration.tcpQuickAck);
        httpServerOptions.setTcpCork(httpConfiguration.tcpCork);
        httpServerOptions.setAcceptBacklog(httpConfiguration.acceptBacklog);
        httpServerOptions.setTcpFastOpen(httpConfiguration.tcpFastOpen);
        httpServerOptions.setCompressionSupported(buildTimeConfig.enableCompression);
        if (buildTimeConfig.compressionLevel.isPresent()) {
            httpServerOptions.setCompressionLevel(buildTimeConfig.compressionLevel.getAsInt());
        }
        httpServerOptions.setDecompressionSupported(buildTimeConfig.enableDecompression);
        httpServerOptions.setMaxInitialLineLength(httpConfiguration.limits.maxInitialLineLength);
        httpServerOptions.setHandle100ContinueAutomatically(httpConfiguration.handle100ContinueAutomatically);

        if (buildTimeConfig.compressors.isPresent()) {
            // Adding defaults too, because mere addition of .addCompressor(brotli) actually
            // overrides the default deflate and gzip capability.
            for (String compressor : buildTimeConfig.compressors.get()) {
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
                    if (buildTimeConfig.compressionLevel.isPresent()) {
                        o.parameters().setQuality(buildTimeConfig.compressionLevel.getAsInt());
                    }
                    httpServerOptions.addCompressor(o);
                } else {
                    Logger.getLogger(HttpServerOptionsUtils.class).errorf("Unknown compressor: %s", compressor);
                }
            }
        }

        if (httpConfiguration.http2) {
            var settings = new Http2Settings();
            if (httpConfiguration.limits.headerTableSize.isPresent()) {
                settings.setHeaderTableSize(httpConfiguration.limits.headerTableSize.getAsLong());
            }
            settings.setPushEnabled(httpConfiguration.http2PushEnabled);
            if (httpConfiguration.limits.maxConcurrentStreams.isPresent()) {
                settings.setMaxConcurrentStreams(httpConfiguration.limits.maxConcurrentStreams.getAsLong());
            }
            if (httpConfiguration.initialWindowSize.isPresent()) {
                settings.setInitialWindowSize(httpConfiguration.initialWindowSize.getAsInt());
            }
            if (httpConfiguration.limits.maxFrameSize.isPresent()) {
                settings.setMaxFrameSize(httpConfiguration.limits.maxFrameSize.getAsInt());
            }
            if (httpConfiguration.limits.maxHeaderListSize.isPresent()) {
                settings.setMaxHeaderListSize(httpConfiguration.limits.maxHeaderListSize.getAsLong());
            }
            httpServerOptions.setInitialSettings(settings);

            // RST attack protection - https://github.com/netty/netty/security/advisories/GHSA-xpw8-rcwv-8f8p
            if (httpConfiguration.limits.rstFloodMaxRstFramePerWindow.isPresent()) {
                httpServerOptions
                        .setHttp2RstFloodMaxRstFramePerWindow(httpConfiguration.limits.rstFloodMaxRstFramePerWindow.getAsInt());
            }
            if (httpConfiguration.limits.rstFloodWindowDuration.isPresent()) {
                httpServerOptions.setHttp2RstFloodWindowDuration(
                        (int) httpConfiguration.limits.rstFloodWindowDuration.get().toSeconds());
                httpServerOptions.setHttp2RstFloodWindowDurationTimeUnit(TimeUnit.SECONDS);
            }

        }

        httpServerOptions.setUseProxyProtocol(httpConfiguration.proxy.useProxyProtocol);
        configureTrafficShapingIfEnabled(httpServerOptions, httpConfiguration);
    }

    private static void configureTrafficShapingIfEnabled(HttpServerOptions httpServerOptions,
            HttpConfiguration httpConfiguration) {
        if (httpConfiguration.trafficShaping.enabled) {
            TrafficShapingOptions options = new TrafficShapingOptions();
            if (httpConfiguration.trafficShaping.checkInterval.isPresent()) {
                options.setCheckIntervalForStats(httpConfiguration.trafficShaping.checkInterval.get().toSeconds());
                options.setCheckIntervalForStatsTimeUnit(TimeUnit.SECONDS);
            }
            if (httpConfiguration.trafficShaping.maxDelay.isPresent()) {
                options.setMaxDelayToWait(httpConfiguration.trafficShaping.maxDelay.get().toSeconds());
                options.setMaxDelayToWaitUnit(TimeUnit.SECONDS);
            }
            if (httpConfiguration.trafficShaping.inboundGlobalBandwidth.isPresent()) {
                options.setInboundGlobalBandwidth(httpConfiguration.trafficShaping.inboundGlobalBandwidth.get().asLongValue());
            }
            if (httpConfiguration.trafficShaping.outboundGlobalBandwidth.isPresent()) {
                options.setOutboundGlobalBandwidth(
                        httpConfiguration.trafficShaping.outboundGlobalBandwidth.get().asLongValue());
            }
            if (httpConfiguration.trafficShaping.peakOutboundGlobalBandwidth.isPresent()) {
                options.setPeakOutboundGlobalBandwidth(
                        httpConfiguration.trafficShaping.peakOutboundGlobalBandwidth.get().asLongValue());
            }
            httpServerOptions.setTrafficShapingOptions(options);
        }
    }

    public static void applyCommonOptionsForManagementInterface(HttpServerOptions options,
            ManagementInterfaceBuildTimeConfig buildTimeConfig,
            ManagementInterfaceConfiguration httpConfiguration,
            List<String> websocketSubProtocols) {
        options.setHost(httpConfiguration.host.orElse("0.0.0.0"));

        int idleTimeout = (int) httpConfiguration.idleTimeout.toMillis();
        options.setIdleTimeout(idleTimeout);
        options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);

        options.setMaxHeaderSize(httpConfiguration.limits.maxHeaderSize.asBigInteger().intValueExact());
        options.setMaxChunkSize(httpConfiguration.limits.maxChunkSize.asBigInteger().intValueExact());
        options.setMaxFormAttributeSize(httpConfiguration.limits.maxFormAttributeSize.asBigInteger().intValueExact());
        options.setMaxFormFields(httpConfiguration.limits.maxFormFields);
        options.setMaxFormBufferedBytes(httpConfiguration.limits.maxFormBufferedBytes.asBigInteger().intValue());
        options.setMaxInitialLineLength(httpConfiguration.limits.maxInitialLineLength);
        options.setWebSocketSubProtocols(websocketSubProtocols);
        options.setAcceptBacklog(httpConfiguration.acceptBacklog);
        options.setCompressionSupported(buildTimeConfig.enableCompression);
        if (buildTimeConfig.compressionLevel.isPresent()) {
            options.setCompressionLevel(buildTimeConfig.compressionLevel.getAsInt());
        }
        options.setDecompressionSupported(buildTimeConfig.enableDecompression);
        options.setHandle100ContinueAutomatically(httpConfiguration.handle100ContinueAutomatically);

        options.setUseProxyProtocol(httpConfiguration.proxy.useProxyProtocol);
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

    private static void setIdleTimeout(HttpConfiguration httpConfiguration, HttpServerOptions options) {
        int idleTimeout = (int) httpConfiguration.idleTimeout.toMillis();
        options.setIdleTimeout(idleTimeout);
        options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
    }

    public static HttpConfiguration.InsecureRequests getInsecureRequestStrategy(HttpBuildTimeConfig buildTimeConfig,
            Optional<HttpConfiguration.InsecureRequests> requests) {
        if (requests.isPresent()) {
            var value = requests.get();
            if (buildTimeConfig.tlsClientAuth == ClientAuth.REQUIRED && value == HttpConfiguration.InsecureRequests.ENABLED) {
                Logger.getLogger(HttpServerOptionsUtils.class).warn(
                        "When configuring TLS client authentication to be required, it is recommended to **NOT** set `quarkus.http.insecure-requests` to `enabled`. "
                                +
                                "You can switch to `redirect` by setting `quarkus.http.insecure-requests=redirect`.");
            }
            return value;
        }
        if (buildTimeConfig.tlsClientAuth == ClientAuth.REQUIRED) {
            Logger.getLogger(HttpServerOptionsUtils.class).info(
                    "TLS client authentication is required, thus disabling insecure requests. " +
                            "You can switch to `redirect` by setting `quarkus.http.insecure-requests=redirect`.");
            return HttpConfiguration.InsecureRequests.DISABLED;
        }
        return HttpConfiguration.InsecureRequests.ENABLED;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Optional<T> or(Optional<T> a, Optional<T> b) {
        return a.isPresent() ? a : b;
    }
}
