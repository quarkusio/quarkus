package io.quarkus.kubernetes.client.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.arc.DefaultBean;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapper;

@Singleton
public class KubernetesSerializationProducer {

    @DefaultBean
    @Singleton
    @Produces
    public KubernetesSerialization kubernetesSerialization(@KubernetesClientObjectMapper ObjectMapper objectMapper) {
        final var kubernetesSerialization = new KubernetesSerialization(objectMapper, false);
        KubernetesClientUtils.scanKubernetesResources().forEach(kubernetesSerialization::registerKubernetesResource);
        return kubernetesSerialization;
    }
}
