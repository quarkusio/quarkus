package io.quarkus.it.kafka.streams;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;
import static java.util.Map.entry;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaSSLTestResource implements QuarkusTestResourceLifecycleManager {

    private static final StrimziKafkaContainer kafka = new StrimziKafkaContainer()
            .withKafkaConfigurationMap(Map.ofEntries(
                    entry("ssl.keystore.location", "/opt/kafka/config/kafka-keystore.p12"),
                    entry("ssl.keystore.password", "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L"),
                    entry("ssl.keystore.type", "PKCS12"),
                    entry("ssl.key.password", "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L"),
                    entry("ssl.truststore.location", "/opt/kafka/config/kafka-truststore.p12"),
                    entry("ssl.truststore.password", "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L"),
                    entry("ssl.truststore.type", "PKCS12"),
                    entry("ssl.endpoint.identification.algorithm=", ""),
                    entry("listener.security.protocol.map", "BROKER1:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL")))
            .withBootstrapServers(c -> String.format("SSL://%s:%s", c.getHost(), c.getMappedPort(KAFKA_PORT)))
            .withCopyFileToContainer(MountableFile.forClasspathResource("ks-keystore.p12"),
                    "/opt/kafka/config/kafka-keystore.p12")
            .withCopyFileToContainer(MountableFile.forClasspathResource("ks-truststore.p12"),
                    "/opt/kafka/config/kafka-truststore.p12");

    public static String getBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    @Override
    public Map<String, String> start() {
        kafka.start();
        // Used by the application
        Map<String, String> properties = new HashMap<>();
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());
        properties.put("ssl-dir", new File("src/main/resources").getAbsolutePath());

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
