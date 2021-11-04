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
        return "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/apis/ccompat/v6";
    }

    public static String getApicurioSchemaRegistryUrl() {
        return "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/apis/registry/v2";
    }

    @Override
    public Map<String, String> start() {
        kafka.start();
        registry = new GenericContainer<>("apicurio/apicurio-registry-mem:2.1.1.Final")
                .withExposedPorts(8080)
                .withEnv("QUARKUS_PROFILE", "prod");
        registry.start();
        Map<String, String> properties = new HashMap<>();
        properties.put("schema.url.confluent",
                "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/apis/ccompat/v6");
        properties.put("schema.url.apicurio",
                "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/apis/registry/v2");
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());
        return properties;
    }

    @Override
    public void stop() {
        registry.stop();
        kafka.close();
    }
}
