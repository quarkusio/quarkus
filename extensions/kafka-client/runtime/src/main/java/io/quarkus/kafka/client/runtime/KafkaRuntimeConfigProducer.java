package io.quarkus.kafka.client.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.ApplicationConfig;
import io.smallrye.common.annotation.Identifier;

@Singleton
public class KafkaRuntimeConfigProducer {

    // not "kafka.", because we also inspect env vars, which start with "KAFKA_"
    private static final String CONFIG_PREFIX = "kafka";

    private static final String GROUP_ID = "group.id";

    @Produces
    @DefaultBean
    @Singleton
    @Identifier("default-kafka-broker")
    public Map<String, Object> createKafkaRuntimeConfig(Config config, ApplicationConfig app) {
        Map<String, Object> result = new HashMap<>();

        for (String propertyName : config.getPropertyNames()) {
            String propertyNameLowerCase = propertyName.toLowerCase();
            if (!propertyNameLowerCase.startsWith(CONFIG_PREFIX)) {
                continue;
            }
            String effectivePropertyName = propertyNameLowerCase.substring(CONFIG_PREFIX.length() + 1).toLowerCase()
                    .replaceAll("[^a-z0-9.]", ".");
            String value = config.getOptionalValue(propertyName, String.class).orElse("");
            result.put(effectivePropertyName, value);
        }

        if (!result.isEmpty() && !result.containsKey(GROUP_ID) && app.name.isPresent()) {
            result.put(GROUP_ID, app.name.get());
        }

        return result;
    }

}
