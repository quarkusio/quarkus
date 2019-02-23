package io.quarkus.example.test;

import java.io.File;
import java.util.Properties;

import io.debezium.kafka.KafkaCluster;
import io.debezium.util.Testing;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private KafkaCluster kafka;

    @Override
    public void start() {
        try {
            Properties props = new Properties();
            props.setProperty("zookeeper.connection.timeout.ms", "10000");
            File directory = Testing.Files.createTestingDirectory("kafka-data", true);
            kafka = new KafkaCluster().withPorts(2182, 19092)
                    .addBrokers(1)
                    .usingDirectory(directory)
                    .deleteDataUponShutdown(true)
                    .withKafkaConfiguration(props)
                    .deleteDataPriorToStartup(true)
                    .startup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        kafka.shutdown();
    }
}
