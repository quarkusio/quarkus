package io.quarkus.it.kafka;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.StrimziKafkaContainer;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private static final StrimziKafkaContainer kafka = new StrimziKafkaContainer();

    public static String getBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    @Override
    public Map<String, String> start() {
        kafka.start();
        return Collections.singletonMap("kafka.bootstrap.servers", kafka.getBootstrapServers());
    }

    @Override
    public void stop() {
        kafka.close();
    }
}
