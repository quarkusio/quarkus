package io.quarkus.it.kafka;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;
import static java.util.Map.entry;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaSASLTestResource implements QuarkusTestResourceLifecycleManager {

    private final StrimziKafkaContainer kafka = new StrimziKafkaContainer()
            .withBrokerId(0)
            .withBootstrapServers(c -> String.format("SASL_PLAINTEXT://%s:%s", c.getHost(),
                    c.getMappedPort(KAFKA_PORT)))
            .withKafkaConfigurationMap(Map.ofEntries(
                    entry("listener.security.protocol.map",
                            "SASL_PLAINTEXT:SASL_PLAINTEXT,BROKER1:PLAINTEXT,PLAINTEXT:PLAINTEXT"),
                    entry("sasl.enabled.mechanisms", "PLAIN"),
                    entry("sasl.mechanism.inter.broker.protocol", "PLAIN"),
                    entry("listener.name.sasl_plaintext.plain.sasl.jaas.config",
                            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                                    "username=\"broker\" " +
                                    "password=\"broker-secret\" " +
                                    "user_broker=\"broker-secret\" " +
                                    "user_client=\"client-secret\";")));

    @Override
    public Map<String, String> start() {
        kafka.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
