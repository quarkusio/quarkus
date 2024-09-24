package io.quarkus.kubernetes.client.runtime;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

public class KubernetesClientUtils {

    private static final String PREFIX = "quarkus.kubernetes-client.";

    private KubernetesClientUtils() {
    }

    public static Config createConfig(KubernetesClientBuildConfig buildConfig) {
        boolean globalTrustAll = ConfigProvider.getConfig().getOptionalValue("quarkus.tls.trust-all", Boolean.class)
                .orElse(false);
        Config base = Config.autoConfigure(null);
        boolean trustAll = buildConfig.trustCerts().isPresent() ? buildConfig.trustCerts().get() : globalTrustAll;
        return new ConfigBuilder()
                .withTrustCerts(trustAll)
                .withWatchReconnectInterval(
                        millisAsInt(buildConfig.watchReconnectInterval()).orElse(base.getWatchReconnectInterval()))
                .withWatchReconnectLimit(buildConfig.watchReconnectLimit().orElse(base.getWatchReconnectLimit()))
                .withConnectionTimeout(millisAsInt(buildConfig.connectionTimeout()).orElse(base.getConnectionTimeout()))
                .withRequestTimeout(millisAsInt(buildConfig.requestTimeout()).orElse(base.getRequestTimeout()))
                .withMasterUrl(buildConfig.apiServerUrl().or(() -> buildConfig.masterUrl()).orElse(base.getMasterUrl()))
                .withNamespace(buildConfig.namespace().orElse(base.getNamespace()))
                .withUsername(buildConfig.username().orElse(base.getUsername()))
                .withPassword(buildConfig.password().orElse(base.getPassword()))
                .withOauthToken(buildConfig.token().orElse(base.getOauthToken()))
                .withCaCertFile(buildConfig.caCertFile().orElse(base.getCaCertFile()))
                .withCaCertData(buildConfig.caCertData().orElse(base.getCaCertData()))
                .withClientCertFile(buildConfig.clientCertFile().orElse(base.getClientCertFile()))
                .withClientCertData(buildConfig.clientCertData().orElse(base.getClientCertData()))
                .withClientKeyFile(buildConfig.clientKeyFile().orElse(base.getClientKeyFile()))
                .withClientKeyData(buildConfig.clientKeyData().orElse(base.getClientKeyData()))
                .withClientKeyPassphrase(buildConfig.clientKeyPassphrase().orElse(base.getClientKeyPassphrase()))
                .withClientKeyAlgo(buildConfig.clientKeyAlgo().orElse(base.getClientKeyAlgo()))
                .withHttpProxy(buildConfig.httpProxy().orElse(base.getHttpProxy()))
                .withHttpsProxy(buildConfig.httpsProxy().orElse(base.getHttpsProxy()))
                .withProxyUsername(buildConfig.proxyUsername().orElse(base.getProxyUsername()))
                .withProxyPassword(buildConfig.proxyPassword().orElse(base.getProxyPassword()))
                .withNoProxy(buildConfig.noProxy().map(list -> list.toArray(new String[0])).orElse(base.getNoProxy()))
                .withHttp2Disable(base.isHttp2Disable())
                .withRequestRetryBackoffInterval(millisAsInt(buildConfig.requestRetryBackoffInterval())
                        .orElse(base.getRequestRetryBackoffInterval()))
                .withRequestRetryBackoffLimit(buildConfig.requestRetryBackoffLimit().orElse(base.getRequestRetryBackoffLimit()))
                .build();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Integer> millisAsInt(Optional<Duration> duration) {
        return duration.map(d -> (int) d.toMillis());
    }

    public static KubernetesClient createClient(KubernetesClientBuildConfig buildConfig) {
        return new KubernetesClientBuilder().withConfig(createConfig(buildConfig)).build();
    }

    public static KubernetesClient createClient() {
        org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();
        Config base = Config.autoConfigure(null);
        return new KubernetesClientBuilder().withConfig(new ConfigBuilder()
                .withTrustCerts(config.getOptionalValue(PREFIX + "trust-certs", Boolean.class).orElse(base.isTrustCerts()))
                .withWatchReconnectLimit(config.getOptionalValue(PREFIX + "watch-reconnect-limit", Integer.class)
                        .orElse(base.getWatchReconnectLimit()))
                .withWatchReconnectInterval((int) config.getOptionalValue(PREFIX + "watch-reconnect-interval", Duration.class)
                        .orElse(Duration.ofMillis(base.getWatchReconnectInterval())).toMillis())
                .withConnectionTimeout((int) config.getOptionalValue(PREFIX + "connection-timeout", Duration.class)
                        .orElse(Duration.ofMillis(base.getConnectionTimeout())).toMillis())
                .withRequestTimeout((int) config.getOptionalValue(PREFIX + "request-timeout", Duration.class)
                        .orElse(Duration.ofMillis(base.getRequestTimeout())).toMillis())
                .withMasterUrl(config.getOptionalValue(PREFIX + "api-server-url", String.class)
                        .or(() -> config.getOptionalValue(PREFIX + "master-url", String.class)).orElse(base.getMasterUrl()))
                .withNamespace(config.getOptionalValue(PREFIX + "namespace", String.class).orElse(base.getNamespace()))
                .withUsername(config.getOptionalValue(PREFIX + "username", String.class).orElse(base.getUsername()))
                .withPassword(config.getOptionalValue(PREFIX + "password", String.class).orElse(base.getPassword()))
                .withCaCertFile(config.getOptionalValue(PREFIX + "ca-cert-file", String.class).orElse(base.getCaCertFile()))
                .withCaCertData(config.getOptionalValue(PREFIX + "ca-cert-data", String.class).orElse(base.getCaCertData()))
                .withClientCertFile(
                        config.getOptionalValue(PREFIX + "client-cert-file", String.class).orElse(base.getClientCertFile()))
                .withClientCertData(
                        config.getOptionalValue(PREFIX + "client-cert-data", String.class).orElse(base.getClientCertData()))
                .withClientKeyFile(
                        config.getOptionalValue(PREFIX + "client-key-file", String.class).orElse(base.getClientKeyFile()))
                .withClientKeyData(
                        config.getOptionalValue(PREFIX + "client-key-data", String.class).orElse(base.getClientKeyData()))
                .withClientKeyPassphrase(config.getOptionalValue(PREFIX + "client-key-passphrase", String.class)
                        .orElse(base.getClientKeyPassphrase()))
                .withClientKeyAlgo(
                        config.getOptionalValue(PREFIX + "client-key-algo", String.class).orElse(base.getClientKeyAlgo()))
                .withHttpProxy(config.getOptionalValue(PREFIX + "http-proxy", String.class).orElse(base.getHttpProxy()))
                .withHttpsProxy(config.getOptionalValue(PREFIX + "https-proxy", String.class).orElse(base.getHttpsProxy()))
                .withProxyUsername(
                        config.getOptionalValue(PREFIX + "proxy-username", String.class).orElse(base.getProxyUsername()))
                .withProxyPassword(
                        config.getOptionalValue(PREFIX + "proxy-password", String.class).orElse(base.getProxyPassword()))
                .withNoProxy(config.getOptionalValue(PREFIX + "no-proxy", String[].class).orElse(base.getNoProxy()))
                .build())
                .build();
    }
}
