package io.quarkus.kubernetes.client;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.kubernetes.client.runtime.KubernetesClientObjectMapperProducer;
import io.quarkus.kubernetes.client.runtime.KubernetesClientProducer;
import io.quarkus.kubernetes.client.runtime.KubernetesSerializationProducer;

/**
 * Allow the provision of beans that customize the default {@link ObjectMapper} used by the KubernetesClient to perform
 * kubernetes-specific serialization and deserialization operations.
 * <p>
 * The resulting {@link ObjectMapper} is used to produce the default {@link KubernetesSerialization} bean, which is in
 * turn used to produce the default {@link io.fabric8.kubernetes.client.KubernetesClient} bean.
 * <p>
 * The following code snippet shows how to provide a KubernetesClientObjectMapperCustomizer:
 *
 * <pre>{@code
 *
 * &#64;Singleton
 * public static class Customizer implements KubernetesClientObjectMapperCustomizer {
 *     @Override
 *     public void customize(ObjectMapper objectMapper) {
 *         objectMapper.setLocale(Locale.ROOT);
 *     }
 * }
 *
 * }</pre>
 *
 * @see KubernetesClientObjectMapperProducer#kubernetesClientObjectMapper(List)
 * @see KubernetesSerializationProducer#kubernetesSerialization(ObjectMapper, Class[])
 * @see KubernetesClientProducer#kubernetesClient(KubernetesSerialization, Config)
 */
public interface KubernetesClientObjectMapperCustomizer {
    void customize(ObjectMapper objectMapper);
}
