package io.quarkus.vertx.http.runtime.options;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ClassPathUtils;
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
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.TrafficShapingOptions;

@SuppressWarnings("OptionalIsPresent")
public class HttpServerOptionsUtils {

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     */
    public static HttpServerOptions createSslOptions(HttpBuildTimeConfig buildTimeConfig, HttpConfiguration httpConfiguration,
            LaunchMode launchMode, List<String> websocketSubProtocols)
            throws IOException {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }

        ServerSslConfig sslConfig = httpConfiguration.ssl;

        final List<Path> keys = new ArrayList<>();
        final List<Path> certificates = new ArrayList<>();
        if (sslConfig.certificate.keyFiles.isPresent()) {
            keys.addAll(sslConfig.certificate.keyFiles.get());
        }
        if (sslConfig.certificate.files.isPresent()) {
            certificates.addAll(sslConfig.certificate.files.get());
        }

        // credentials provider
        Map<String, String> credentials = Map.of();
        if (sslConfig.certificate.credentialsProvider.isPresent()) {
            String beanName = sslConfig.certificate.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = sslConfig.certificate.credentialsProvider.get();
            credentials = credentialsProvider.getCredentials(name);
        }
        final Optional<Path> keyStoreFile = sslConfig.certificate.keyStoreFile;
        final Optional<String> keyStorePassword = getCredential(sslConfig.certificate.keyStorePassword, credentials,
                sslConfig.certificate.keyStorePasswordKey);
        final Optional<String> keyStoreKeyPassword = getCredential(sslConfig.certificate.keyStoreKeyPassword, credentials,
                sslConfig.certificate.keyStoreKeyPasswordKey);
        final Optional<Path> trustStoreFile = sslConfig.certificate.trustStoreFile;
        final Optional<String> trustStorePassword = getCredential(sslConfig.certificate.trustStorePassword, credentials,
                sslConfig.certificate.trustStorePasswordKey);
        final HttpServerOptions serverOptions = new HttpServerOptions();

        //ssl
        if (JdkSSLEngineOptions.isAlpnAvailable()) {
            serverOptions.setUseAlpn(httpConfiguration.http2);
            if (httpConfiguration.http2) {
                serverOptions.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
            }
        }
        setIdleTimeout(httpConfiguration, serverOptions);

        if (!certificates.isEmpty() && !keys.isEmpty()) {
            createPemKeyCertOptions(certificates, keys, serverOptions);
        } else if (keyStoreFile.isPresent()) {

            KeyStoreOptions options = createKeyStoreOptions(
                    keyStoreFile.get(),
                    keyStorePassword.orElse("password"),
                    sslConfig.certificate.keyStoreFileType,
                    sslConfig.certificate.keyStoreProvider,
                    sslConfig.certificate.keyStoreKeyAlias,
                    keyStoreKeyPassword);
            serverOptions.setKeyCertOptions(options);
        }

        if (trustStoreFile.isPresent()) {
            if (!trustStorePassword.isPresent()) {
                throw new IllegalArgumentException("No trust store password provided");
            }
            KeyStoreOptions options = createKeyStoreOptions(
                    trustStoreFile.get(),
                    trustStorePassword.get(),
                    sslConfig.certificate.trustStoreFileType,
                    sslConfig.certificate.trustStoreProvider,
                    sslConfig.certificate.trustStoreCertAlias,
                    Optional.empty());
            serverOptions.setTrustOptions(options);
        }

        for (String cipher : sslConfig.cipherSuites.orElse(Collections.emptyList())) {
            serverOptions.addEnabledCipherSuite(cipher);
        }

        serverOptions.setEnabledSecureTransportProtocols(sslConfig.protocols);
        serverOptions.setSsl(true);
        serverOptions.setSni(sslConfig.sni);
        int sslPort = httpConfiguration.determineSslPort(launchMode);
        // -2 instead of -1 (see http) to have vert.x assign two different random ports if both http and https shall be random
        serverOptions.setPort(sslPort == 0 ? -2 : sslPort);
        serverOptions.setClientAuth(buildTimeConfig.tlsClientAuth);

        applyCommonOptions(serverOptions, buildTimeConfig, httpConfiguration, websocketSubProtocols);

        return serverOptions;
    }

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     */
    public static HttpServerOptions createSslOptionsForManagementInterface(ManagementInterfaceBuildTimeConfig buildTimeConfig,
            ManagementInterfaceConfiguration httpConfiguration,
            LaunchMode launchMode, List<String> websocketSubProtocols)
            throws IOException {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }

        ServerSslConfig sslConfig = httpConfiguration.ssl;

        final List<Path> keys = new ArrayList<>();
        final List<Path> certificates = new ArrayList<>();
        if (sslConfig.certificate.keyFiles.isPresent()) {
            keys.addAll(sslConfig.certificate.keyFiles.get());
        }
        if (sslConfig.certificate.files.isPresent()) {
            certificates.addAll(sslConfig.certificate.files.get());
        }

        // credentials provider
        Map<String, String> credentials = Map.of();
        if (sslConfig.certificate.credentialsProvider.isPresent()) {
            String beanName = sslConfig.certificate.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = sslConfig.certificate.credentialsProvider.get();
            credentials = credentialsProvider.getCredentials(name);
        }
        final Optional<Path> keyStoreFile = sslConfig.certificate.keyStoreFile;
        final Optional<String> keyStorePassword = getCredential(sslConfig.certificate.keyStorePassword, credentials,
                sslConfig.certificate.keyStorePasswordKey);
        final Optional<String> keyStoreKeyPassword = getCredential(sslConfig.certificate.keyStoreKeyPassword, credentials,
                sslConfig.certificate.keyStoreKeyPasswordKey);
        final Optional<Path> trustStoreFile = sslConfig.certificate.trustStoreFile;
        final Optional<String> trustStorePassword = getCredential(sslConfig.certificate.trustStorePassword, credentials,
                sslConfig.certificate.trustStorePasswordKey);
        final HttpServerOptions serverOptions = new HttpServerOptions();

        //ssl
        if (JdkSSLEngineOptions.isAlpnAvailable()) {
            serverOptions.setUseAlpn(true);
            serverOptions.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
        }
        int idleTimeout = (int) httpConfiguration.idleTimeout.toMillis();
        serverOptions.setIdleTimeout(idleTimeout);
        serverOptions.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);

        if (!certificates.isEmpty() && !keys.isEmpty()) {
            createPemKeyCertOptions(certificates, keys, serverOptions);
        } else if (keyStoreFile.isPresent()) {
            KeyStoreOptions options = createKeyStoreOptions(
                    keyStoreFile.get(),
                    keyStorePassword.orElse("password"),
                    sslConfig.certificate.keyStoreFileType,
                    sslConfig.certificate.keyStoreProvider,
                    sslConfig.certificate.keyStoreKeyAlias,
                    keyStoreKeyPassword);
            serverOptions.setKeyCertOptions(options);
        }

        if (trustStoreFile.isPresent()) {
            if (!trustStorePassword.isPresent()) {
                throw new IllegalArgumentException("No trust store password provided");
            }
            KeyStoreOptions options = createKeyStoreOptions(
                    trustStoreFile.get(),
                    trustStorePassword.get(),
                    sslConfig.certificate.trustStoreFileType,
                    sslConfig.certificate.trustStoreProvider,
                    sslConfig.certificate.trustStoreCertAlias,
                    Optional.empty());
            serverOptions.setTrustOptions(options);
        }

        for (String cipher : sslConfig.cipherSuites.orElse(Collections.emptyList())) {
            serverOptions.addEnabledCipherSuite(cipher);
        }

        serverOptions.setEnabledSecureTransportProtocols(sslConfig.protocols);

        serverOptions.setSsl(true);
        serverOptions.setSni(sslConfig.sni);
        int sslPort = httpConfiguration.determinePort(launchMode);
        // -2 instead of -1 (see http) to have vert.x assign two different random ports if both http and https shall be random
        serverOptions.setPort(sslPort == 0 ? -2 : sslPort);
        serverOptions.setClientAuth(buildTimeConfig.tlsClientAuth);

        applyCommonOptionsForManagementInterface(serverOptions, buildTimeConfig, httpConfiguration, websocketSubProtocols);

        return serverOptions;
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

    private static KeyStoreOptions createKeyStoreOptions(Path path, String password, Optional<String> fileType,
            Optional<String> provider, Optional<String> alias, Optional<String> aliasPassword) throws IOException {
        final String type;
        if (fileType.isPresent()) {
            type = fileType.get().toLowerCase();
        } else {
            type = findKeystoreFileType(path);
        }

        byte[] data = getFileContent(path);
        KeyStoreOptions options = new KeyStoreOptions()
                .setPassword(password)
                .setValue(Buffer.buffer(data))
                .setType(type.toUpperCase())
                .setProvider(provider.orElse(null))
                .setAlias(alias.orElse(null))
                .setAliasPassword(aliasPassword.orElse(null));
        return options;
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

    private static void createPemKeyCertOptions(List<Path> certFile, List<Path> keyFile,
            HttpServerOptions serverOptions) throws IOException {

        if (certFile.size() != keyFile.size()) {
            throw new ConfigurationException("Invalid certificate configuration - `files` and `keyFiles` must have the "
                    + "same number of elements");
        }

        List<Buffer> certificates = new ArrayList<>();
        List<Buffer> keys = new ArrayList<>();

        for (Path p : certFile) {
            final byte[] cert = getFileContent(p);
            certificates.add(Buffer.buffer(cert));
        }

        for (Path p : keyFile) {
            final byte[] key = getFileContent(p);
            keys.add(Buffer.buffer(key));
        }

        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                .setCertValues(certificates)
                .setKeyValues(keys);
        serverOptions.setPemKeyCertOptions(pemKeyCertOptions);
    }

    private static String findKeystoreFileType(Path storePath) {
        final String pathName = storePath.toString();
        if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
            return "pkcs12";
        } else {
            // assume jks
            return "jks";
        }
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
}
