package io.quarkus.it.kafka.streams.norocksdb;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private static final StrimziKafkaContainer kafka = new StrimziKafkaContainer();

    public static String getBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    @Override
    public Map<String, String> start() {
        kafka.start();
        Map<String, String> properties = new HashMap<>();
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());
        return properties;
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.close();
        }
    }
}
