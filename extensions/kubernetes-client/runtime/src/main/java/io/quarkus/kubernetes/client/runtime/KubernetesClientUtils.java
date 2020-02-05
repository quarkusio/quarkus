package io.quarkus.kubernetes.client.runtime;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesClientUtils {

    public static Config createConfig(KubernetesClientBuildConfig buildConfig) {
        Config base = new Config();
        return new ConfigBuilder()
                .withTrustCerts(buildConfig.trustCerts)
                .withWatchReconnectInterval((int) buildConfig.watchReconnectInterval.toMillis())
                .withWatchReconnectLimit(buildConfig.watchReconnectLimit)
                .withConnectionTimeout((int) buildConfig.connectionTimeout.toMillis())
                .withRequestTimeout((int) buildConfig.requestTimeout.toMillis())
                .withRollingTimeout(buildConfig.rollingTimeout.toMillis())
                .withMasterUrl(buildConfig.masterUrl.orElse(base.getMasterUrl()))
                .withNamespace(buildConfig.namespace.orElse(base.getNamespace()))
                .withUsername(buildConfig.username.orElse(base.getUsername()))
                .withPassword(buildConfig.password.orElse(base.getPassword()))
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
                .withNoProxy(buildConfig.noProxy.isPresent() ? buildConfig.noProxy.get() : base.getNoProxy())
                .build();
    }

    public static KubernetesClient createClient(KubernetesClientBuildConfig buildConfig) {
        return new DefaultKubernetesClient(createConfig(buildConfig));
    }
}
