package io.quarkus.kubernetes.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class KubernetesClientProducer {

    private volatile KubernetesClientBuildConfig buildConfig;

    @DefaultBean
    @Singleton
    @Produces
    public Config config() {
        return KubernetesClientUtils.createConfig(buildConfig);
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
