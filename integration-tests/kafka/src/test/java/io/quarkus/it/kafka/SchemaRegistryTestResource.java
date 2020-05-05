package io.quarkus.it.kafka;

import java.util.Collections;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SchemaRegistryTestResource implements QuarkusTestResourceLifecycleManager {

    public GenericContainer<?> registry = new GenericContainer<>("apicurio/apicurio-registry-mem:1.1.0.Final")
            .withExposedPorts(8080)
            .withEnv("QUARKUS_PROFILE", "prod")
            .withEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:19092")
            .withEnv("APPLICATION_ID", "registry_id")
            .withEnv("APPLICATION_SERVER", "localhost:9000");

    @Override
    public Map<String, String> start() {
        registry.start();
        return Collections
                .singletonMap("schema.url",
                        "http://" + registry.getContainerIpAddress() + ":" + registry.getMappedPort(8080) + "/ccompat");
    }

    @Override
    public void stop() {
        registry.stop();
    }
}
