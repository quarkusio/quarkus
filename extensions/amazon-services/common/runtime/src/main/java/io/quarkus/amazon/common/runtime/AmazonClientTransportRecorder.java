package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.Http2Configuration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@Recorder
public class AmazonClientTransportRecorder {

    public RuntimeValue<SdkAsyncHttpClient.Builder> configureAsync(String clientName,
            RuntimeValue<NettyHttpClientConfig> asyncConfigRuntime) {
        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder();
        NettyHttpClientConfig asyncConfig = asyncConfigRuntime.getValue();
        validateNettyClientConfig(clientName, asyncConfig);

        builder.connectionAcquisitionTimeout(asyncConfig.connectionAcquisitionTimeout);
        builder.connectionMaxIdleTime(asyncConfig.connectionMaxIdleTime);
        builder.connectionTimeout(asyncConfig.connectionTimeout);
        asyncConfig.connectionTimeToLive.ifPresent(builder::connectionTimeToLive);
        builder.maxConcurrency(asyncConfig.maxConcurrency);
        builder.maxPendingConnectionAcquires(asyncConfig.maxPendingConnectionAcquires);
        builder.protocol(asyncConfig.protocol);
        builder.readTimeout(asyncConfig.readTimeout);
        builder.writeTimeout(asyncConfig.writeTimeout);
        asyncConfig.sslProvider.ifPresent(builder::sslProvider);
        builder.useIdleConnectionReaper(asyncConfig.useIdleConnectionReaper);

        if (asyncConfig.http2.initialWindowSize.isPresent() || asyncConfig.http2.maxStreams.isPresent()) {
            Http2Configuration.Builder http2Builder = Http2Configuration.builder();
            asyncConfig.http2.initialWindowSize.ifPresent(http2Builder::initialWindowSize);
            asyncConfig.http2.maxStreams.ifPresent(http2Builder::maxStreams);
            asyncConfig.http2.healthCheckPingPeriod.ifPresent(http2Builder::healthCheckPingPeriod);
            builder.http2Configuration(http2Builder.build());
        }

        if (asyncConfig.proxy.enabled && asyncConfig.proxy.endpoint.isPresent()) {
            software.amazon.awssdk.http.nio.netty.ProxyConfiguration.Builder proxyBuilder = software.amazon.awssdk.http.nio.netty.ProxyConfiguration
                    .builder().scheme(asyncConfig.proxy.endpoint.get().getScheme())
                    .host(asyncConfig.proxy.endpoint.get().getHost())
                    .nonProxyHosts(new HashSet<>(asyncConfig.proxy.nonProxyHosts.orElse(Collections.emptyList())));

            if (asyncConfig.proxy.endpoint.get().getPort() != -1) {
                proxyBuilder.port(asyncConfig.proxy.endpoint.get().getPort());
            }
            builder.proxyConfiguration(proxyBuilder.build());
        }

        getTlsKeyManagersProvider(asyncConfig.tlsKeyManagersProvider).ifPresent(builder::tlsKeyManagersProvider);
        getTlsTrustManagersProvider(asyncConfig.tlsTrustManagersProvider).ifPresent(builder::tlsTrustManagersProvider);

        if (asyncConfig.eventLoop.override) {
            SdkEventLoopGroup.Builder eventLoopBuilder = SdkEventLoopGroup.builder();
            asyncConfig.eventLoop.numberOfThreads.ifPresent(eventLoopBuilder::numberOfThreads);
            if (asyncConfig.eventLoop.threadNamePrefix.isPresent()) {
                eventLoopBuilder.threadFactory(
                        new ThreadFactoryBuilder().threadNamePrefix(asyncConfig.eventLoop.threadNamePrefix.get()).build());
            }
            builder.eventLoopGroupBuilder(eventLoopBuilder);
        }

        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<SdkHttpClient.Builder> configureSyncUrlConnectionHttpClient(String clientName,
            RuntimeValue<SyncHttpClientConfig> syncConfigRuntime) {
        SyncHttpClientConfig syncConfig = syncConfigRuntime.getValue();
        validateTlsKeyManagersProvider(clientName, syncConfig.tlsKeyManagersProvider, "sync");
        validateTlsTrustManagersProvider(clientName, syncConfig.tlsTrustManagersProvider, "sync");
        UrlConnectionHttpClient.Builder builder = UrlConnectionHttpClient.builder();
        builder.connectionTimeout(syncConfig.connectionTimeout);
        builder.socketTimeout(syncConfig.socketTimeout);
        getTlsKeyManagersProvider(syncConfig.tlsKeyManagersProvider).ifPresent(builder::tlsKeyManagersProvider);
        getTlsTrustManagersProvider(syncConfig.tlsTrustManagersProvider).ifPresent(builder::tlsTrustManagersProvider);
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<SdkHttpClient.Builder> configureSyncApacheHttpClient(String clientName,
            RuntimeValue<SyncHttpClientConfig> syncConfigRuntime) {
        SyncHttpClientConfig syncConfig = syncConfigRuntime.getValue();
        validateTlsKeyManagersProvider(clientName, syncConfig.tlsKeyManagersProvider, "sync");
        validateTlsTrustManagersProvider(clientName, syncConfig.tlsTrustManagersProvider, "sync");
        Builder builder = ApacheHttpClient.builder();
        validateApacheClientConfig(clientName, syncConfig);

        builder.connectionTimeout(syncConfig.connectionTimeout);
        builder.connectionAcquisitionTimeout(syncConfig.apache.connectionAcquisitionTimeout);
        builder.connectionMaxIdleTime(syncConfig.apache.connectionMaxIdleTime);
        syncConfig.apache.connectionTimeToLive.ifPresent(builder::connectionTimeToLive);
        builder.expectContinueEnabled(syncConfig.apache.expectContinueEnabled);
        builder.maxConnections(syncConfig.apache.maxConnections);
        builder.socketTimeout(syncConfig.socketTimeout);
        builder.useIdleConnectionReaper(syncConfig.apache.useIdleConnectionReaper);

        if (syncConfig.apache.proxy.enabled && syncConfig.apache.proxy.endpoint.isPresent()) {
            ProxyConfiguration.Builder proxyBuilder = ProxyConfiguration.builder()
                    .endpoint(syncConfig.apache.proxy.endpoint.get());
            syncConfig.apache.proxy.username.ifPresent(proxyBuilder::username);
            syncConfig.apache.proxy.password.ifPresent(proxyBuilder::password);
            syncConfig.apache.proxy.nonProxyHosts.ifPresent(c -> c.forEach(proxyBuilder::addNonProxyHost));
            syncConfig.apache.proxy.ntlmDomain.ifPresent(proxyBuilder::ntlmDomain);
            syncConfig.apache.proxy.ntlmWorkstation.ifPresent(proxyBuilder::ntlmWorkstation);
            syncConfig.apache.proxy.preemptiveBasicAuthenticationEnabled
                    .ifPresent(proxyBuilder::preemptiveBasicAuthenticationEnabled);

            builder.proxyConfiguration(proxyBuilder.build());
        }
        getTlsKeyManagersProvider(syncConfig.tlsKeyManagersProvider).ifPresent(builder::tlsKeyManagersProvider);
        getTlsTrustManagersProvider(syncConfig.tlsTrustManagersProvider).ifPresent(builder::tlsTrustManagersProvider);
        return new RuntimeValue<>(builder);
    }

    private Optional<TlsKeyManagersProvider> getTlsKeyManagersProvider(TlsKeyManagersProviderConfig config) {
        if (config.fileStore != null && config.fileStore.path.isPresent() && config.fileStore.type.isPresent()) {
            return Optional.of(config.type.create(config));
        }
        return Optional.empty();
    }

    private Optional<TlsTrustManagersProvider> getTlsTrustManagersProvider(TlsTrustManagersProviderConfig config) {
        if (config.fileStore != null && config.fileStore.path.isPresent() && config.fileStore.type.isPresent()) {
            return Optional.of(config.type.create(config));
        }
        return Optional.empty();
    }

    private void validateApacheClientConfig(String extension, SyncHttpClientConfig config) {
        if (config.apache.maxConnections <= 0) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.sync-client.max-connections may not be negative or zero.", extension));
        }
        if (config.apache.proxy.enabled) {
            config.apache.proxy.endpoint.ifPresent(uri -> validateProxyEndpoint(extension, uri, "sync"));
        }
    }

    private void validateNettyClientConfig(String extension, NettyHttpClientConfig config) {
        if (config.maxConcurrency <= 0) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.async-client.max-concurrency may not be negative or zero.", extension));
        }

        if (config.http2.maxStreams.isPresent() && config.http2.maxStreams.get() <= 0) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.async-client.http2.max-streams may not be negative.", extension));
        }

        if (config.http2.initialWindowSize.isPresent() && config.http2.initialWindowSize.getAsInt() <= 0) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.async-client.http2.initial-window-size may not be negative.", extension));
        }

        if (config.maxPendingConnectionAcquires <= 0) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.async-client.max-pending-connection-acquires may not be negative or zero.",
                            extension));
        }
        if (config.eventLoop.override) {
            if (config.eventLoop.numberOfThreads.isPresent() && config.eventLoop.numberOfThreads.getAsInt() <= 0) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.async-client.event-loop.number-of-threads may not be negative or zero.",
                                extension));
            }
        }
        if (config.proxy.enabled) {
            config.proxy.endpoint.ifPresent(uri -> validateProxyEndpoint(extension, uri, "async"));
        }

        validateTlsKeyManagersProvider(extension, config.tlsKeyManagersProvider, "async");
        validateTlsTrustManagersProvider(extension, config.tlsTrustManagersProvider, "async");
    }

    private void validateProxyEndpoint(String extension, URI endpoint, String clientType) {
        if (StringUtils.isBlank(endpoint.getScheme())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.%s-client.proxy.endpoint (%s) - scheme must be specified",
                            extension, clientType, endpoint.toString()));
        }
        if (StringUtils.isBlank(endpoint.getHost())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.%s-client.proxy.endpoint (%s) - host must be specified",
                            extension, clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getUserInfo())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.%s-client.proxy.endpoint (%s) - user info is not supported.",
                            extension, clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getPath())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.%s-client.proxy.endpoint (%s) - path is not supported.",
                            extension, clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getQuery())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.%s-client.proxy.endpoint (%s) - query is not supported.",
                            extension, clientType, endpoint.toString()));
        }
        if (StringUtils.isNotBlank(endpoint.getFragment())) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.%s-client.proxy.endpoint (%s) - fragment is not supported.",
                            extension, clientType, endpoint.toString()));
        }
    }

    private void validateTlsKeyManagersProvider(String extension, TlsKeyManagersProviderConfig config, String clientType) {
        if (config != null && config.type == TlsKeyManagersProviderType.FILE_STORE) {
            validateFileStore(extension, clientType, "key", config.fileStore);
        }
    }

    private void validateTlsTrustManagersProvider(String extension, TlsTrustManagersProviderConfig config, String clientType) {
        if (config != null && config.type == TlsTrustManagersProviderType.FILE_STORE) {
            validateFileStore(extension, clientType, "trust", config.fileStore);
        }
    }

    private void validateFileStore(String extension, String clientType, String storeType,
            FileStoreTlsManagersProviderConfig fileStore) {
        if (fileStore == null) {
            throw new RuntimeConfigurationError(
                    String.format(
                            "quarkus.%s.%s-client.tls-%s-managers-provider.file-store must be specified if 'FILE_STORE' provider type is used",
                            extension, clientType, storeType));
        } else {
            if (!fileStore.password.isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format(
                                "quarkus.%s.%s-client.tls-%s-managers-provider.file-store.path should not be empty if 'FILE_STORE' provider is used.",
                                extension, clientType, storeType));
            }
            if (!fileStore.type.isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format(
                                "quarkus.%s.%s-client.tls-%s-managers-provider.file-store.type should not be empty if 'FILE_STORE' provider is used.",
                                extension, clientType, storeType));
            }
            if (!fileStore.password.isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format(
                                "quarkus.%s.%s-client.tls-%s-managers-provider.file-store.password should not be empty if 'FILE_STORE' provider is used.",
                                extension, clientType, storeType));
            }
        }
    }
}
