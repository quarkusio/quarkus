package io.quarkus.kafka.client.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.ApplicationConfig;

@Dependent
public class KafkaRuntimeConfigProducer {

    // not "kafka.", because we also inspect env vars, which start with "KAFKA_"
    private static final String CONFIG_PREFIX = "kafka";

    private static final String GROUP_ID = "group.id";

    @Produces
    @DefaultBean
    @ApplicationScoped
    @Named("default-kafka-broker")
    public Map<String, Object> createKafkaRuntimeConfig(ApplicationConfig app) {
        Map<String, Object> properties = new HashMap<>();
        final Config config = ConfigProvider.getConfig();

        StreamSupport
                .stream(config.getPropertyNames().spliterator(), false)
                .map(String::toLowerCase)
                .filter(name -> name.startsWith(CONFIG_PREFIX))
                .distinct()
                .sorted()
                .forEach(name -> {
                    final String key = name.substring(CONFIG_PREFIX.length() + 1).toLowerCase().replaceAll("[^a-z0-9.]", ".");
                    final String value = config.getOptionalValue(name, String.class).orElse("");
                    properties.put(key, value);
                });

        if (!properties.containsKey(GROUP_ID) && app.name.isPresent()) {
            properties.put(GROUP_ID, app.name.get());
        }

        return properties;
    }

}
