package io.quarkus.it.kafka;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.debezium.kafka.KafkaCluster;
import io.debezium.util.Testing;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private KafkaCluster kafka;

    @Override
    public Map<String, String> start() {
        try {
            Properties props = new Properties();
            props.setProperty("zookeeper.connection.timeout.ms", "45000");
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
        Map<String, String> result = new HashMap<>();
        // make the service binding root known
        result.put("quarkus.kubernetes-service-binding.root", Paths.get("").resolve("src").resolve("test").resolve("resources")
                .resolve("k8s-sb").toAbsolutePath().toString());
        return result;
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.shutdown();
        }
    }
}
