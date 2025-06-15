package io.quarkus.kubernetes.client.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.arc.DefaultBean;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapper;
import io.quarkus.kubernetes.client.KubernetesResources;

@Singleton
public class KubernetesSerializationProducer {

    @DefaultBean
    @Singleton
    @Produces
    public KubernetesSerialization kubernetesSerialization(@KubernetesClientObjectMapper ObjectMapper objectMapper,
            @KubernetesResources Class[] kubernetesResources) {
        final var kubernetesSerialization = new KubernetesSerialization(objectMapper, false);
        for (var kubernetesResource : kubernetesResources) {
            kubernetesSerialization.registerKubernetesResource(kubernetesResource);
        }
        return kubernetesSerialization;
    }
}
