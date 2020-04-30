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

@Dependent
public class KafkaRuntimeConfigProducer {

    private String configPrefix = "kafka";

    @Produces
    @DefaultBean
    @ApplicationScoped
    @Named("default-kafka-broker")
    public Map<String, Object> createKafkaRuntimeConfig() {
        Map<String, Object> properties = new HashMap<>();
        final Config config = ConfigProvider.getConfig();

        StreamSupport
                .stream(config.getPropertyNames().spliterator(), false)
                .map(String::toLowerCase)
                .filter(name -> name.startsWith(configPrefix))
                .distinct()
                .sorted()
                .forEach(name -> {
                    final String key = name.substring(configPrefix.length() + 1).toLowerCase().replaceAll("[^a-z0-9.]", ".");
                    final String value = config.getOptionalValue(name, String.class).orElse("");
                    properties.put(key, value);
                });

        return properties;
    }

}
