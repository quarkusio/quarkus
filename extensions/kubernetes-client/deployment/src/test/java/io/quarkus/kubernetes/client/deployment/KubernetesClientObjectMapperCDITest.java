package io.quarkus.kubernetes.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapper;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapperCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class KubernetesClientObjectMapperCDITest {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    @KubernetesClientObjectMapper
    ObjectMapper objectMapper;

    @Test
    public void kubernetesClientObjectMapperCustomizer() throws JsonProcessingException {
        final var result = objectMapper.readValue("{\"quarkusName\":\"the-name\"}", ObjectMeta.class);
        assertEquals("the-name", result.getName());
    }

    @Test
    public void kubernetesClientUsesCustomizedObjectMapper() {
        final var result = kubernetesClient.getKubernetesSerialization().unmarshal("{\"quarkusName\":\"the-name\"}",
                ObjectMeta.class);
        assertEquals("the-name", result.getName());
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.kubernetes-client.devservices.enabled", "false");

    @Singleton
    public static class Customizer implements KubernetesClientObjectMapperCustomizer {
        @Override
        public void customize(ObjectMapper objectMapper) {
            objectMapper.addMixIn(ObjectMeta.class, ObjectMetaMixin.class);
        }

        private static final class ObjectMetaMixin {
            @JsonProperty("quarkusName")
            String name;
        }
    }
}
