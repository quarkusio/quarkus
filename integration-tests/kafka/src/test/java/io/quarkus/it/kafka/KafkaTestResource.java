package io.quarkus.it.kafka;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import io.debezium.kafka.KafkaCluster;
import io.debezium.kafka.KafkaServer;
import io.debezium.util.Testing;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import kafka.server.RunningAsBroker;

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

        kafka.server.KafkaServer server = extract(kafka);
        await().until(() -> server.brokerState().currentState() == RunningAsBroker.state());
        server.logger().underlying().info("Broker 'kafka' started");

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    static kafka.server.KafkaServer extract(KafkaCluster cluster) {
        Field kafkaServersField;
        Field serverField;
        try {
            kafkaServersField = cluster.getClass().getDeclaredField("kafkaServers");
            kafkaServersField.setAccessible(true);
            Map<Integer, KafkaServer> map = (Map<Integer, KafkaServer>) kafkaServersField.get(cluster);
            KafkaServer server = map.get(1);
            serverField = KafkaServer.class.getDeclaredField("server");
            serverField.setAccessible(true);
            return (kafka.server.KafkaServer) serverField.get(server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
