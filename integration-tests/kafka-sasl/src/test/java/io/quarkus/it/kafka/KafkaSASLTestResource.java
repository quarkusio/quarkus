package io.quarkus.it.kafka;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaSASLTestResource implements QuarkusTestResourceLifecycleManager {

    private final StrimziKafkaContainer kafka = new StrimziKafkaContainer()
            .withServerProperties(MountableFile.forClasspathResource("server.properties"))
            .withBootstrapServers(
                    container -> String.format("SASL_PLAINTEXT://%s:%s", container.getHost(),
                            container.getMappedPort(KAFKA_PORT)));

    @Override
    public Map<String, String> start() {
        kafka.start();
        // Used by the test
        System.setProperty("bootstrap.servers", kafka.getBootstrapServers());
        // Used by the application
        Map<String, String> properties = new HashMap<>();
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());

        return properties;
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.close();
        }
        System.clearProperty("boostrap.servers");
    }

}
