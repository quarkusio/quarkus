package io.quarkus.it.kafka;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaAndSchemaRegistryTestResource implements QuarkusTestResourceLifecycleManager {

    private static GenericContainer<?> registry;

    public static String getConfluentSchemaRegistryUrl() {
        return "http://" + registry.getHost() + ":" + registry.getMappedPort(8080) + "/api/ccompat";
    }

    public static String getApicurioSchemaRegistryUrl() {
        return "http://" + registry.getHost() + ":" + registry.getMappedPort(8080) + "/api";
    }

    @Override
    public Map<String, String> start() {
        registry = new GenericContainer<>("apicurio/apicurio-registry-mem:1.2.2.Final")
                .withExposedPorts(8080)
                .withEnv("QUARKUS_PROFILE", "prod");
        registry.start();
        Map<String, String> properties = new HashMap<>();
        properties.put("schema.url.confluent",
                "http://" + registry.getHost() + ":" + registry.getMappedPort(8080) + "/api/ccompat");
        properties.put("schema.url.apicurio",
                "http://" + registry.getHost() + ":" + registry.getMappedPort(8080) + "/api");
        return properties;
    }

    @Override
    public void stop() {
        registry.stop();
    }
}
