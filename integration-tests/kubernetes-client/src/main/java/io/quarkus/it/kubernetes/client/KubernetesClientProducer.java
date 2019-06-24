package io.quarkus.it.kubernetes.client;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Dependent
public class KubernetesClientProducer {

    @Produces
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }
}
