package io.quarkus.it.kafka;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SchemaRegistryTestResource implements QuarkusTestResourceLifecycleManager {

    public GenericContainer<?> registry = new GenericContainer<>("apicurio/apicurio-registry-mem:1.2.2.Final")
            .withExposedPorts(8080)
            .withEnv("QUARKUS_PROFILE", "prod")
            .withEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:19092")
            .withEnv("APPLICATION_ID", "registry_id")
            .withEnv("APPLICATION_SERVER", "localhost:9000");

    @Override
    public Map<String, String> start() {
        registry.start();
        Map<String, String> properties = new HashMap<>();
        properties.put("schema.url.confluent",
                "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/api/ccompat");
        properties.put("schema.url.apicurio",
                "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/api");

        return properties;
    }

    @Override
    public void stop() {
        registry.stop();
    }
}
