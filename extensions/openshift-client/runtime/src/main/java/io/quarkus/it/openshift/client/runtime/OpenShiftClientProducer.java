package io.quarkus.it.openshift.client.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.arc.DefaultBean;

@Singleton
public class OpenShiftClientProducer {

    @DefaultBean
    @Singleton
    @Produces
    public OpenShiftClient openShiftClient(@Named("kubernetes-client") KubernetesClient kubernetesClient) {
        return kubernetesClient.adapt(OpenShiftClient.class);
    }
}
