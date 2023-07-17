package io.quarkus.kafka.client.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.ApplicationConfig;
import io.smallrye.common.annotation.Identifier;

@Singleton
public class KafkaRuntimeConfigProducer {

    // not "kafka.", because we also inspect env vars, which start with "KAFKA_"
    private static final String CONFIG_PREFIX = "kafka";
    private static final String UI_CONFIG_PREFIX = CONFIG_PREFIX + ".ui";
    private static final String GROUP_ID = "group.id";

    @Produces
    @DefaultBean
    @Singleton
    @Identifier("default-kafka-broker")
    public Map<String, Object> createKafkaRuntimeConfig(Config config, ApplicationConfig app) {
        Map<String, Object> result = new HashMap<>();

        for (String propertyName : config.getPropertyNames()) {
            String propertyNameLowerCase = propertyName.toLowerCase();
            if (propertyNameLowerCase.startsWith(UI_CONFIG_PREFIX)) {
                config.getOptionalValue(propertyName, String.class).orElse("");
            }
            if (!propertyNameLowerCase.startsWith(CONFIG_PREFIX) || propertyNameLowerCase.startsWith(UI_CONFIG_PREFIX)) {
                continue;
            }
            // Replace _ by . - This is because Kafka properties tend to use . and env variables use _ for every special
            // character. So, replace _ with .
            String effectivePropertyName = propertyNameLowerCase.substring(CONFIG_PREFIX.length() + 1).toLowerCase()
                    .replace("_", ".");
            String value = config.getOptionalValue(propertyName, String.class).orElse("");
            result.put(effectivePropertyName, value);
        }

        if (!result.isEmpty() && !result.containsKey(GROUP_ID) && app.name.isPresent()) {
            result.put(GROUP_ID, app.name.get());
        }

        return result;
    }

}
