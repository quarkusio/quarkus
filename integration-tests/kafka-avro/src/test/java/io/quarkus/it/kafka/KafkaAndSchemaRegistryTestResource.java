package io.quarkus.it.kafka;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.StrimziKafkaContainer;

public class KafkaAndSchemaRegistryTestResource implements QuarkusTestResourceLifecycleManager {

    private static final StrimziKafkaContainer kafka = new StrimziKafkaContainer();

    private static GenericContainer<?> registry;

    public static String getBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    public static String getConfluentSchemaRegistryUrl() {
        return "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/api/ccompat";
    }

    public static String getApicurioSchemaRegistryUrl() {
        return "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/api";
    }

    @Override
    public Map<String, String> start() {
        kafka.start();
        registry = new GenericContainer<>("apicurio/apicurio-registry-mem:1.2.2.Final")
                .withExposedPorts(8080)
                .withEnv("QUARKUS_PROFILE", "prod");
        registry.start();
        Map<String, String> properties = new HashMap<>();
        properties.put("schema.url.confluent",
                "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/api/ccompat");
        properties.put("schema.url.apicurio",
                "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/api");
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());
        return properties;
    }

    @Override
    public void stop() {
        registry.stop();
        kafka.close();
    }
}
