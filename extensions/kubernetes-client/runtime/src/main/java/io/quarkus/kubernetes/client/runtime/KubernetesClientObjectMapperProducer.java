package io.quarkus.kubernetes.client.runtime;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapper;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapperCustomizer;

@Singleton
public class KubernetesClientObjectMapperProducer {

    @KubernetesClientObjectMapper
    @DefaultBean
    @Priority(Integer.MIN_VALUE)
    @Singleton
    @Produces
    public ObjectMapper kubernetesClientObjectMapper(@All List<KubernetesClientObjectMapperCustomizer> customizers) {
        final var result = new ObjectMapper();
        for (var customizer : customizers) {
            customizer.customize(result);
        }
        return result;
    }
}
