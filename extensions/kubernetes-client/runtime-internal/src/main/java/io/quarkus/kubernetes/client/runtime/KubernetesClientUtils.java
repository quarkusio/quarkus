package io.quarkus.kubernetes.client.runtime;

import java.time.Duration;

import org.eclipse.microprofile.config.ConfigProvider;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.TlsConfig;

public class KubernetesClientUtils {

    private static final String PREFIX = "quarkus.kubernetes-client.";

    public static Config createConfig(KubernetesClientBuildConfig buildConfig, TlsConfig tlsConfig) {
        Config base = Config.autoConfigure(null);
        boolean trustAll = buildConfig.trustCerts.isPresent() ? buildConfig.trustCerts.get() : tlsConfig.trustAll;
        return new ConfigBuilder()
                .withTrustCerts(trustAll)
                .withWatchReconnectInterval((int) buildConfig.watchReconnectInterval.toMillis())
                .withWatchReconnectLimit(buildConfig.watchReconnectLimit)
                .withConnectionTimeout((int) buildConfig.connectionTimeout.toMillis())
                .withRequestTimeout((int) buildConfig.requestTimeout.toMillis())
                .withRollingTimeout(buildConfig.rollingTimeout.toMillis())
                .withMasterUrl(buildConfig.masterUrl.orElse(base.getMasterUrl()))
                .withNamespace(buildConfig.namespace.orElse(base.getNamespace()))
                .withUsername(buildConfig.username.orElse(base.getUsername()))
                .withPassword(buildConfig.password.orElse(base.getPassword()))
                .withOauthToken(buildConfig.token.orElse(base.getOauthToken()))
                .withCaCertFile(buildConfig.caCertFile.orElse(base.getCaCertFile()))
                .withCaCertData(buildConfig.caCertData.orElse(base.getCaCertData()))
                .withClientCertFile(buildConfig.clientCertFile.orElse(base.getClientCertFile()))
                .withClientCertData(buildConfig.clientCertData.orElse(base.getClientCertData()))
                .withClientKeyFile(buildConfig.clientKeyFile.orElse(base.getClientKeyFile()))
                .withClientKeyData(buildConfig.clientKeyData.orElse(base.getClientKeyData()))
                .withClientKeyPassphrase(buildConfig.clientKeyPassphrase.orElse(base.getClientKeyPassphrase()))
                .withClientKeyAlgo(buildConfig.clientKeyAlgo.orElse(base.getClientKeyAlgo()))
                .withHttpProxy(buildConfig.httpProxy.orElse(base.getHttpProxy()))
                .withHttpsProxy(buildConfig.httpsProxy.orElse(base.getHttpsProxy()))
                .withProxyUsername(buildConfig.proxyUsername.orElse(base.getProxyUsername()))
                .withProxyPassword(buildConfig.proxyPassword.orElse(base.getProxyPassword()))
                .withNoProxy(buildConfig.noProxy.orElse(base.getNoProxy()))
                .withHttp2Disable(base.isHttp2Disable())
                .build();
    }

    public static KubernetesClient createClient(KubernetesClientBuildConfig buildConfig, TlsConfig tlsConfig) {
        return new DefaultKubernetesClient(createConfig(buildConfig, tlsConfig));
    }

    public static KubernetesClient createClient() {
        org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();
        Config base = Config.autoConfigure(null);
        return new DefaultKubernetesClient(new ConfigBuilder()
                .withTrustCerts(config.getOptionalValue(PREFIX + "trust-certs", Boolean.class).orElse(base.isTrustCerts()))
                .withWatchReconnectLimit(config.getOptionalValue(PREFIX + "watch-reconnect-limit", Integer.class)
                        .orElse(base.getWatchReconnectLimit()))
                .withWatchReconnectInterval((int) config.getOptionalValue(PREFIX + "watch-reconnect-interval", Duration.class)
                        .orElse(Duration.ofMillis(base.getWatchReconnectInterval())).toMillis())
                .withConnectionTimeout((int) config.getOptionalValue(PREFIX + "connection-timeout", Duration.class)
                        .orElse(Duration.ofMillis(base.getConnectionTimeout())).toMillis())
                .withRequestTimeout((int) config.getOptionalValue(PREFIX + "request-timeout", Duration.class)
                        .orElse(Duration.ofMillis(base.getRequestTimeout())).toMillis())
                .withRollingTimeout((int) config.getOptionalValue(PREFIX + "rolling-timeout", Duration.class)
                        .orElse(Duration.ofMillis(base.getRollingTimeout())).toMillis())
                .withMasterUrl(config.getOptionalValue(PREFIX + "master-url", String.class).orElse(base.getMasterUrl()))
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
                .build());
    }
}
