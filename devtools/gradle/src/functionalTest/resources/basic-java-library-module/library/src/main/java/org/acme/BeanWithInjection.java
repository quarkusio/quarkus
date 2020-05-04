package org.acme;

import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BeanWithInjection {
    @Inject
    KubernetesClient kubernetesClient;

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }
}
