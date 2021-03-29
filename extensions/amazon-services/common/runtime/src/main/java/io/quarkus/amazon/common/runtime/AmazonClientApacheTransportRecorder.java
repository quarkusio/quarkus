package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

@Recorder
public class AmazonClientApacheTransportRecorder extends AbstractAmazonClientTransportRecorder {

    @SuppressWarnings("rawtypes")
    @Override
    public RuntimeValue<SdkHttpClient.Builder> configureSync(String clientName,
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

    private void validateApacheClientConfig(String extension, SyncHttpClientConfig config) {
        if (config.apache.maxConnections <= 0) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.sync-client.max-connections may not be negative or zero.", extension));
        }
        if (config.apache.proxy.enabled) {
            config.apache.proxy.endpoint.ifPresent(uri -> validateProxyEndpoint(extension, uri, "sync"));
        }
    }
}
