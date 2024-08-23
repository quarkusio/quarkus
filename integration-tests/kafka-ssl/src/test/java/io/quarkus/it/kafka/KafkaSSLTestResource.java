package io.quarkus.it.kafka;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaSSLTestResource implements QuarkusTestResourceLifecycleManager {

    private final StrimziKafkaContainer kafka = new StrimziKafkaContainer()
            .withBootstrapServers(c -> String.format("SSL://%s:%s", c.getHost(), c.getMappedPort(KAFKA_PORT)))
            .withServerProperties(MountableFile.forClasspathResource("server.properties"))
            .withCopyFileToContainer(MountableFile.forHostPath("target/certs/kafka-keystore.p12"),
                    "/opt/kafka/config/kafka-keystore.p12")
            .withCopyFileToContainer(MountableFile.forHostPath("target/certs/kafka-truststore.p12"),
                    "/opt/kafka/config/kafka-truststore.p12");

    private Map<String, String> initProps;

    @Override
    public void init(Map<String, String> initArgs) {
        initProps = initArgs;
    }

    @Override
    public Map<String, String> start() {
        kafka.start();
        // Used by the test
        System.setProperty("bootstrap.servers", kafka.getBootstrapServers());
        // Used by the application
        Map<String, String> properties = new HashMap<>(initProps);
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());
        properties.put("ssl-dir", new File("target/certs").getAbsolutePath());

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
