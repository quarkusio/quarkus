package org.acme;

import io.fabric8.kubernetes.client.KubernetesClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BeanWithInjection {
    @Inject
    KubernetesClient kubernetesClient;

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }
}
