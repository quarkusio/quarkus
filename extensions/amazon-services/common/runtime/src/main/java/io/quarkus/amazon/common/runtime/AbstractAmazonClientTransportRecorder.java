package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.util.Optional;

import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.StringUtils;

public abstract class AbstractAmazonClientTransportRecorder {

    @SuppressWarnings("rawtypes")
    public RuntimeValue<SdkHttpClient.Builder> configureSync(String clientName,
            RuntimeValue<SyncHttpClientConfig> syncConfigRuntime) {
        throw new IllegalStateException("Configuring a sync client is not supported by " + this.getClass().getName());
    }

    @SuppressWarnings("rawtypes")
    public RuntimeValue<SdkAsyncHttpClient.Builder> configureAsync(String clientName,
            RuntimeValue<NettyHttpClientConfig> asyncConfigRuntime) {
        throw new IllegalStateException("Configuring an async client is not supported by " + this.getClass().getName());
    }

    protected Optional<TlsKeyManagersProvider> getTlsKeyManagersProvider(TlsKeyManagersProviderConfig config) {
        if (config.fileStore != null && config.fileStore.path.isPresent() && config.fileStore.type.isPresent()) {
            return Optional.of(config.type.create(config));
        }
        return Optional.empty();
    }

    protected Optional<TlsTrustManagersProvider> getTlsTrustManagersProvider(TlsTrustManagersProviderConfig config) {
        if (config.fileStore != null && config.fileStore.path.isPresent() && config.fileStore.type.isPresent()) {
            return Optional.of(config.type.create(config));
        }
        return Optional.empty();
    }

    protected void validateProxyEndpoint(String extension, URI endpoint, String clientType) {
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

    protected void validateTlsKeyManagersProvider(String extension, TlsKeyManagersProviderConfig config, String clientType) {
        if (config != null && config.type == TlsKeyManagersProviderType.FILE_STORE) {
            validateFileStore(extension, clientType, "key", config.fileStore);
        }
    }

    protected void validateTlsTrustManagersProvider(String extension, TlsTrustManagersProviderConfig config,
            String clientType) {
        if (config != null && config.type == TlsTrustManagersProviderType.FILE_STORE) {
            validateFileStore(extension, clientType, "trust", config.fileStore);
        }
    }

    protected void validateFileStore(String extension, String clientType, String storeType,
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
