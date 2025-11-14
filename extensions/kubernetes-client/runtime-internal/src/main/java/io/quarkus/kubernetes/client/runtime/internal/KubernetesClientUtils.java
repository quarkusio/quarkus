package io.quarkus.kubernetes.client.runtime.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Config base;
        if (buildConfig.kubeconfigFile().isPresent()) {
            String kubeconfigPath = buildConfig.kubeconfigFile().get();
            try {
                String kubeconfig = Files.readString(Path.of(kubeconfigPath));
                base = Config.fromKubeconfig(kubeconfig);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read kubeconfig file: " + kubeconfigPath, e);
            }
        } else {
            base = Config.autoConfigure(null);
        }
        boolean trustAll = buildConfig.trustCerts().isPresent() ? buildConfig.trustCerts().get() : globalTrustAll;
        final var configBuilder = new ConfigBuilder(base).withTrustCerts(trustAll);
        buildConfig.watchReconnectInterval().ifPresent(d -> configBuilder.withWatchReconnectInterval(millisAsInt(d)));
        buildConfig.watchReconnectLimit().ifPresent(configBuilder::withWatchReconnectLimit);
        buildConfig.connectionTimeout().ifPresent(d -> configBuilder.withConnectionTimeout(millisAsInt(d)));
        buildConfig.requestTimeout().ifPresent(d -> configBuilder.withRequestTimeout(millisAsInt(d)));
        buildConfig.apiServerUrl().or(buildConfig::masterUrl).ifPresent(configBuilder::withMasterUrl);
        buildConfig.namespace().ifPresent(configBuilder::withNamespace);
        buildConfig.username().ifPresent(configBuilder::withUsername);
        buildConfig.password().ifPresent(configBuilder::withPassword);
        buildConfig.token().ifPresent(configBuilder::withOauthToken);
        buildConfig.caCertFile().ifPresent(configBuilder::withCaCertFile);
        buildConfig.caCertData().ifPresent(configBuilder::withCaCertData);
        buildConfig.clientCertFile().ifPresent(configBuilder::withClientCertFile);
        buildConfig.clientCertData().ifPresent(configBuilder::withClientCertData);
        buildConfig.clientKeyFile().ifPresent(configBuilder::withClientKeyFile);
        buildConfig.clientKeyData().ifPresent(configBuilder::withClientKeyData);
        buildConfig.clientKeyAlgo().ifPresent(configBuilder::withClientKeyAlgo);
        buildConfig.clientKeyPassphrase().ifPresent(configBuilder::withClientKeyPassphrase);
        buildConfig.httpProxy().ifPresent(configBuilder::withHttpProxy);
        buildConfig.httpsProxy().ifPresent(configBuilder::withHttpsProxy);
        buildConfig.proxyUsername().ifPresent(configBuilder::withProxyUsername);
        buildConfig.proxyPassword().ifPresent(configBuilder::withProxyPassword);
        buildConfig.noProxy().ifPresent(list -> list.toArray(new String[0]));
        buildConfig.requestRetryBackoffInterval().ifPresent(d -> configBuilder.withRequestRetryBackoffInterval(millisAsInt(d)));
        buildConfig.requestRetryBackoffLimit().ifPresent(configBuilder::withRequestRetryBackoffLimit);
        return configBuilder.build();
    }

    private static int millisAsInt(Duration duration) {
        return (int) duration.toMillis();
    }

    public static KubernetesClient createClient(KubernetesClientBuildConfig buildConfig) {
        return new KubernetesClientBuilder().withConfig(createConfig(buildConfig)).build();
    }

    public static KubernetesClient createClient() {
        org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();
        Config base = Config.autoConfigure(null);
        final var configBuilder = new ConfigBuilder(base);
        optional(config, "trust-certs", Boolean.class).ifPresent(configBuilder::withTrustCerts);
        optional(config, "watch-reconnect-limit", Integer.class).ifPresent(configBuilder::withWatchReconnectLimit);
        optional(config, "watch-reconnect-interval", Duration.class)
                .map(KubernetesClientUtils::millisAsInt)
                .ifPresent(configBuilder::withWatchReconnectInterval);
        optional(config, "connection-timeout", Duration.class)
                .map(KubernetesClientUtils::millisAsInt)
                .ifPresent(configBuilder::withConnectionTimeout);
        optional(config, "request-timeout", Duration.class)
                .map(KubernetesClientUtils::millisAsInt)
                .ifPresent(configBuilder::withRequestTimeout);
        optional(config, "api-server-url", String.class)
                .or(() -> optional(config, "master-url", String.class))
                .ifPresent(configBuilder::withMasterUrl);
        optional(config, "namespace", String.class).ifPresent(configBuilder::withNamespace);
        optional(config, "username", String.class).ifPresent(configBuilder::withUsername);
        optional(config, "password", String.class).ifPresent(configBuilder::withPassword);
        optional(config, "ca-cert-file", String.class).ifPresent(configBuilder::withCaCertFile);
        optional(config, "ca-cert-data", String.class).ifPresent(configBuilder::withCaCertData);
        optional(config, "client-cert-file", String.class).ifPresent(configBuilder::withClientCertFile);
        optional(config, "client-cert-data", String.class).ifPresent(configBuilder::withClientCertData);
        optional(config, "client-key-file", String.class).ifPresent(configBuilder::withClientKeyFile);
        optional(config, "client-key-data", String.class).ifPresent(configBuilder::withClientKeyData);
        optional(config, "client-key-passphrase", String.class).ifPresent(configBuilder::withClientKeyPassphrase);
        optional(config, "client-key-algo", String.class).ifPresent(configBuilder::withClientKeyAlgo);
        optional(config, "http-proxy", String.class).ifPresent(configBuilder::withHttpProxy);
        optional(config, "https-proxy", String.class).ifPresent(configBuilder::withHttpsProxy);
        optional(config, "proxy-username", String.class).ifPresent(configBuilder::withProxyUsername);
        optional(config, "proxy-password", String.class).ifPresent(configBuilder::withProxyPassword);
        optional(config, "no-proxy", String[].class).ifPresent(configBuilder::withNoProxy);
        return new KubernetesClientBuilder().withConfig(configBuilder.build()).build();
    }

    private static <T> Optional<T> optional(org.eclipse.microprofile.config.Config config, String key, Class<T> valueType) {
        return config.getOptionalValue(PREFIX + key, valueType);
    }
}
