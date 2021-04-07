package io.quarkus.it.kafka;

import static io.quarkus.it.kafka.KafkaTestResource.extract;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.config.SaslConfigs;

import io.debezium.kafka.KafkaCluster;
import io.debezium.util.Testing;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import kafka.server.KafkaServer;
import kafka.server.RunningAsBroker;

public class KafkaSASLTestResource implements QuarkusTestResourceLifecycleManager {

    private KafkaCluster kafka;

    @Override
    public Map<String, String> start() {
        try {
            File directory = Testing.Files.createTestingDirectory("kafka-data-sasl", true);

            Properties props = new Properties();
            props.setProperty("zookeeper.connection.timeout.ms", "45000");
            props.setProperty("listener.security.protocol.map", "CLIENT:SASL_PLAINTEXT");
            props.setProperty("listeners", "CLIENT://:19094");
            props.setProperty("inter.broker.listener.name", "CLIENT");

            props.setProperty("sasl.enabled.mechanisms", "PLAIN");
            props.setProperty("sasl.mechanism.inter.broker.protocol", "PLAIN");

            final String jaasConf = "org.apache.kafka.common.security.plain.PlainLoginModule required" +
                    " username=broker password=broker-secret" +
                    " user_broker=broker-secret user_client=client-secret;";
            props.setProperty("listener.name.client.plain." + SaslConfigs.SASL_JAAS_CONFIG, jaasConf);

            kafka = new KafkaCluster()
                    .withPorts(2184, 19094)
                    .addBrokers(1)
                    .usingDirectory(directory)
                    .deleteDataUponShutdown(true)
                    .withKafkaConfiguration(props)
                    .deleteDataPriorToStartup(true)
                    .startup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        KafkaServer server = extract(kafka);
        await().until(() -> server.brokerState().currentState() == RunningAsBroker.state());
        server.logger().underlying().info("Broker 'kafka-sasl' started");

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.shutdown();
        }
    }

}
