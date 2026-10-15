package io.quarkus.kubernetes.client.runtime;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import io.quarkus.kubernetes.client.KubernetesClientJsonMapperBuilderCustomizer;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Singleton
public class KubernetesClientObjectMapperProducer {

    @KubernetesClientObjectMapper
    @DefaultBean
    @Priority(Integer.MIN_VALUE)
    @Singleton
    @Produces
    public ObjectMapper kubernetesClientObjectMapper(@All List<KubernetesClientJsonMapperBuilderCustomizer> customizers) {
        final var builder = JsonMapper.builder();
        for (var customizer : customizers) {
            customizer.customize(builder);
        }
        return builder.build();
    }
}
