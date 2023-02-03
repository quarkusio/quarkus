package io.quarkus.kubernetes.client.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.TlsConfig;

@Singleton
public class KubernetesConfigProducer {

    @DefaultBean
    @Singleton
    @Produces
    public Config config(KubernetesClientBuildConfig buildConfig, TlsConfig tlsConfig) {
        return KubernetesClientUtils.createConfig(buildConfig, tlsConfig);
    }
}
