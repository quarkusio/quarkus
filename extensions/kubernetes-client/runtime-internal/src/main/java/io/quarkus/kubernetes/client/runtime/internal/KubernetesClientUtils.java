package io.quarkus.kubernetes.client.runtime.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

public class KubernetesClientUtils {

    private static final String PREFIX = "quarkus.kubernetes-client.";

    private KubernetesClientUtils() {
    }

    public static Config createConfig(KubernetesClientConfig clientConfig) {
        io.smallrye.config.Config config = io.smallrye.config.Config.get();
        boolean globalTrustAll = config.getOptionalValue("quarkus.tls.trust-all", Boolean.class).orElse(false);
        Config base;
        if (clientConfig.kubeconfigFile().isPresent()) {
            String kubeconfigPath = clientConfig.kubeconfigFile().get();
            try {
                String kubeconfig = Files.readString(Path.of(kubeconfigPath));
                base = Config.fromKubeconfig(kubeconfig);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read kubeconfig file: " + kubeconfigPath, e);
            }
        } else {
            base = Config.autoConfigure(null);
        }
        boolean trustAll = clientConfig.trustCerts().isPresent() ? clientConfig.trustCerts().get() : globalTrustAll;
        final var configBuilder = new ConfigBuilder(base).withTrustCerts(trustAll);
        clientConfig.watchReconnectInterval().ifPresent(d -> configBuilder.withWatchReconnectInterval(millisAsInt(d)));
        clientConfig.watchReconnectLimit().ifPresent(configBuilder::withWatchReconnectLimit);
        clientConfig.connectionTimeout().ifPresent(d -> configBuilder.withConnectionTimeout(millisAsInt(d)));
        clientConfig.requestTimeout().ifPresent(d -> configBuilder.withRequestTimeout(millisAsInt(d)));
        clientConfig.apiServerUrl().ifPresent(configBuilder::withMasterUrl);
        clientConfig.namespace().ifPresent(configBuilder::withNamespace);
        clientConfig.username().ifPresent(configBuilder::withUsername);
        clientConfig.password().ifPresent(configBuilder::withPassword);
        clientConfig.token().ifPresent(configBuilder::withOauthToken);
        clientConfig.caCertFile().ifPresent(configBuilder::withCaCertFile);
        clientConfig.caCertData().ifPresent(configBuilder::withCaCertData);
        clientConfig.clientCertFile().ifPresent(configBuilder::withClientCertFile);
        clientConfig.clientCertData().ifPresent(configBuilder::withClientCertData);
        clientConfig.clientKeyFile().ifPresent(configBuilder::withClientKeyFile);
        clientConfig.clientKeyData().ifPresent(configBuilder::withClientKeyData);
        clientConfig.clientKeyAlgo().ifPresent(configBuilder::withClientKeyAlgo);
        clientConfig.clientKeyPassphrase().ifPresent(configBuilder::withClientKeyPassphrase);
        clientConfig.httpProxy().ifPresent(configBuilder::withHttpProxy);
        clientConfig.httpsProxy().ifPresent(configBuilder::withHttpsProxy);
        clientConfig.proxyUsername().ifPresent(configBuilder::withProxyUsername);
        clientConfig.proxyPassword().ifPresent(configBuilder::withProxyPassword);
        clientConfig.noProxy().ifPresent(list -> configBuilder.withNoProxy(list.toArray(new String[0])));
        clientConfig.requestRetryBackoffInterval()
                .ifPresent(d -> configBuilder.withRequestRetryBackoffInterval(millisAsInt(d)));
        clientConfig.requestRetryBackoffLimit().ifPresent(configBuilder::withRequestRetryBackoffLimit);
        return configBuilder.build();
    }

    public static Config createConfig() {
        io.smallrye.config.Config config = io.smallrye.config.Config.get();
        boolean globalTrustAll = config.getOptionalValue("quarkus.tls.trust-all", Boolean.class).orElse(false);
        Config base;
        Optional<String> kubeconfigFile = optional(config, "kubeconfig-file", String.class);
        if (kubeconfigFile.isPresent()) {
            String kubeconfigPath = kubeconfigFile.get();
            try {
                String kubeconfig = Files.readString(Path.of(kubeconfigPath));
                base = Config.fromKubeconfig(kubeconfig);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read kubeconfig file: " + kubeconfigPath, e);
            }
        } else {
            base = Config.autoConfigure(null);
        }
        boolean trustAll = optional(config, "trust-certs", Boolean.class).orElse(globalTrustAll);
        final var configBuilder = new ConfigBuilder(base).withTrustCerts(trustAll);
        optional(config, "watch-reconnect-interval", Duration.class)
                .ifPresent(d -> configBuilder.withWatchReconnectInterval(millisAsInt(d)));
        optional(config, "watch-reconnect-limit", Integer.class).ifPresent(configBuilder::withWatchReconnectLimit);
        optional(config, "connection-timeout", Duration.class)
                .ifPresent(d -> configBuilder.withConnectionTimeout(millisAsInt(d)));
        optional(config, "request-timeout", Duration.class)
                .ifPresent(d -> configBuilder.withRequestTimeout(millisAsInt(d)));
        optional(config, "api-server-url", String.class)
                .or(() -> optional(config, "master-url", String.class))
                .ifPresent(configBuilder::withMasterUrl);
        optional(config, "namespace", String.class).ifPresent(configBuilder::withNamespace);
        optional(config, "username", String.class).ifPresent(configBuilder::withUsername);
        optional(config, "password", String.class).ifPresent(configBuilder::withPassword);
        optional(config, "token", String.class).ifPresent(configBuilder::withOauthToken);
        optional(config, "ca-cert-file", String.class).ifPresent(configBuilder::withCaCertFile);
        optional(config, "ca-cert-data", String.class).ifPresent(configBuilder::withCaCertData);
        optional(config, "client-cert-file", String.class).ifPresent(configBuilder::withClientCertFile);
        optional(config, "client-cert-data", String.class).ifPresent(configBuilder::withClientCertData);
        optional(config, "client-key-file", String.class).ifPresent(configBuilder::withClientKeyFile);
        optional(config, "client-key-data", String.class).ifPresent(configBuilder::withClientKeyData);
        optional(config, "client-key-algo", String.class).ifPresent(configBuilder::withClientKeyAlgo);
        optional(config, "client-key-passphrase", String.class).ifPresent(configBuilder::withClientKeyPassphrase);
        optional(config, "http-proxy", String.class).ifPresent(configBuilder::withHttpProxy);
        optional(config, "https-proxy", String.class).ifPresent(configBuilder::withHttpsProxy);
        optional(config, "proxy-username", String.class).ifPresent(configBuilder::withProxyUsername);
        optional(config, "proxy-password", String.class).ifPresent(configBuilder::withProxyPassword);
        optional(config, "no-proxy", String[].class).ifPresent(configBuilder::withNoProxy);
        optional(config, "request-retry-backoff-interval", Duration.class)
                .ifPresent(d -> configBuilder.withRequestRetryBackoffInterval(millisAsInt(d)));
        optional(config, "request-retry-backoff-limit", Integer.class).ifPresent(configBuilder::withRequestRetryBackoffLimit);
        return configBuilder.build();
    }

    public static KubernetesClient createClient(KubernetesClientConfig buildConfig) {
        return new KubernetesClientBuilder().withConfig(createConfig(buildConfig)).build();
    }

    public static KubernetesClient createClient() {
        return new KubernetesClientBuilder().withConfig(createConfig()).build();
    }

    private static <T> Optional<T> optional(org.eclipse.microprofile.config.Config config, String key, Class<T> valueType) {
        return config.getOptionalValue(PREFIX + key, valueType);
    }

    private static int millisAsInt(Duration duration) {
        return (int) duration.toMillis();
    }
}
