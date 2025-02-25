package io.quarkus.kubernetes.client.runtime;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import io.quarkus.kubernetes.client.KubernetesConfigCustomizer;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientUtils;

@Singleton
public class KubernetesConfigProducer {

    @DefaultBean
    @Singleton
    @Produces
    public Config config(KubernetesClientBuildConfig buildConfig,
            @All List<KubernetesConfigCustomizer> customizers) {
        var result = KubernetesClientUtils.createConfig(buildConfig);
        for (KubernetesConfigCustomizer customizer : customizers) {
            customizer.customize(result);
        }
        return result;
    }
}
