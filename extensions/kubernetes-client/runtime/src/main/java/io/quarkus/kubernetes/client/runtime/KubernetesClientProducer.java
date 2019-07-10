package io.quarkus.kubernetes.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class KubernetesClientProducer {

    private KubernetesClientBuildConfig buildConfig;

    @DefaultBean
    @Singleton
    @Produces
    public Config config() {
        Config base = Config.autoConfigure(null);
        return new ConfigBuilder(base)
                .withTrustCerts(buildConfig.trustCerts)
                .withWatchReconnectInterval((int) buildConfig.watchReconnectInterval.toMillis())
                .withWatchReconnectLimit(buildConfig.watchReconnectLimit)
                .withConnectionTimeout((int) buildConfig.connectionTimeout.toMillis())
                .withRequestTimeout((int) buildConfig.requestTimeout.toMillis())
                .withRollingTimeout(buildConfig.rollingTimeout.toMillis())
                .withMasterUrl(or(buildConfig.masterUrl, base.getMasterUrl()))
                .withNamespace(or(buildConfig.namespace, base.getNamespace()))
                .withUsername(or(buildConfig.username, base.getUsername()))
                .withPassword(or(buildConfig.password, base.getPassword()))
                .withCaCertFile(or(buildConfig.caCertFile, base.getCaCertFile()))
                .withCaCertData(or(buildConfig.caCertData, base.getCaCertData()))
                .withClientCertFile(or(buildConfig.clientCertFile, base.getCaCertFile()))
                .withClientCertData(or(buildConfig.clientCertData, base.getCaCertData()))
                .withClientKeyFile(or(buildConfig.clientKeyFile, base.getClientKeyFile()))
                .withClientKeyData(or(buildConfig.clientKeyData, base.getClientKeyData()))
                .withClientKeyPassphrase(or(buildConfig.clientKeyPassphrase, base.getClientKeyPassphrase()))
                .withClientKeyAlgo(or(buildConfig.clientKeyAlgo, base.getClientKeyAlgo()))
                .withHttpProxy(or(buildConfig.httpProxy, base.getHttpProxy()))
                .withHttpsProxy(or(buildConfig.httpsProxy, base.getHttpsProxy()))
                .withProxyUsername(or(buildConfig.proxyUsername, base.getProxyUsername()))
                .withProxyPassword(or(buildConfig.proxyPassword, base.getProxyPassword()))
                .withNoProxy(buildConfig.noProxy.size() > 0 ? buildConfig.noProxy.toArray(new String[0]) : base.getNoProxy())
                .build();

    }

    private String or(String value, String fallback) {
        if (value.isEmpty()) {
            return fallback;
        } else {
            return value;
        }
    }

    @DefaultBean
    @Singleton
    @Produces
    public KubernetesClient kubernetesClient(Config config) {
        return new DefaultKubernetesClient(config);
    }

    public void setKubernetesClientBuildConfig(KubernetesClientBuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }
}
