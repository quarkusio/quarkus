package io.quarkus.kubernetes.client.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.arc.DefaultBean;

@Singleton
public class KubernetesClientProducer {

    private KubernetesClient client;

    @DefaultBean
    @Singleton
    @Produces
    public KubernetesClient kubernetesClient(KubernetesSerialization kubernetesSerialization, Config config) {
        client = new KubernetesClientBuilder().withKubernetesSerialization(kubernetesSerialization).withConfig(config)
                .build();
        return client;
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.close();
        }
    }
}
