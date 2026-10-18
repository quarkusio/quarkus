package io.quarkus.pulsar;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import com.google.common.base.CaseFormat;

import io.quarkus.arc.DefaultBean;
import io.smallrye.common.annotation.Identifier;

@Singleton
public class PulsarRuntimeConfigProducer {

    @Produces
    @DefaultBean
    @Singleton
    @Identifier("default-pulsar-client")
    public Map<String, Object> createPulsarClientRuntimeConfig(Config config) {
        return getMapFromConfig(config, "pulsar.client");
    }

    @Produces
    @DefaultBean
    @Singleton
    @Identifier("default-pulsar-consumer")
    public Map<String, Object> createPulsarConsumerRuntimeConfig(Config config) {
        return getMapFromConfig(config, "pulsar.consumer");
    }

    @Produces
    @DefaultBean
    @Singleton
    @Identifier("default-pulsar-producer")
    public Map<String, Object> createPulsarProducerRuntimeConfig(Config config) {
        return getMapFromConfig(config, "pulsar.producer");
    }

    @Produces
    @DefaultBean
    @Singleton
    @Identifier("default-pulsar-admin")
    public Map<String, Object> createPulsarAdminRuntimeConfig(Config config) {
        return getMapFromConfig(config, "pulsar.admin");
    }

    // visible for testing
    public static Map<String, Object> getMapFromConfig(Config config, String prefix) {
        Map<String, Object> result = new HashMap<>();
        String upperCasePrefix = prefix.toUpperCase().replaceAll("\\.", "_");

        for (String originalKey : config.getPropertyNames()) {
            if (!originalKey.startsWith(prefix) && !originalKey.startsWith(upperCasePrefix)) {
                continue;
            }

            String effectivePropertyName = originalKey.substring(prefix.length() + 1);
            if (originalKey.contains("_") || allCaps(originalKey)) {
                effectivePropertyName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, effectivePropertyName);
            }
            String value = config.getOptionalValue(originalKey, String.class).orElse("");
            result.put(effectivePropertyName, value);
        }

        return result;
    }

    public static boolean allCaps(String key) {
        return key.toUpperCase().equals(key);
    }

}
