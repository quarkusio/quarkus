package io.quarkus.it.kafka;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.jboss.logging.Logger;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaSASLTestResource implements QuarkusTestResourceLifecycleManager {

    private final SaslStrimziKafkaContainer kafka = new SaslStrimziKafkaContainer()
            .withCopyFileToContainer(MountableFile.forClasspathResource("server.properties"),
                    "/opt/kafka/config/server.properties");

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

    /**
     * Use a fixed port container to ease SASL configuration.
     */
    static class SaslStrimziKafkaContainer extends FixedHostPortGenericContainer<SaslStrimziKafkaContainer> {

        private static final org.jboss.logging.Logger LOGGER = Logger
                .getLogger(SaslStrimziKafkaContainer.class.getName());

        private static final String STARTER_SCRIPT = "/testcontainers_start.sh";
        private static final int KAFKA_PORT = 9092;
        private static final int ZOOKEEPER_PORT = 2181;
        private static final String LATEST_KAFKA_VERSION;

        private int kafkaExposedPort;
        private static final List<String> supportedKafkaVersions = new ArrayList<>(3);

        static {
            InputStream inputStream = io.strimzi.StrimziKafkaContainer.class.getResourceAsStream("/kafka-versions.txt");
            InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            try (BufferedReader bufferedReader = new BufferedReader(streamReader)) {
                String kafkaVersion;
                while ((kafkaVersion = bufferedReader.readLine()) != null) {
                    supportedKafkaVersions.add(kafkaVersion);
                }
            } catch (IOException e) {
                LOGGER.error("Unable to load the supported Kafka versions", e);
            }

            // sort kafka version from low to high
            Collections.sort(supportedKafkaVersions);

            LATEST_KAFKA_VERSION = supportedKafkaVersions.get(supportedKafkaVersions.size() - 1);
        }

        public SaslStrimziKafkaContainer(final String version) {
            super("quay.io/strimzi/kafka:" + version);
            super.withNetwork(Network.SHARED);

            // exposing kafka port from the container
            withExposedPorts(KAFKA_PORT);
            withFixedExposedPort(KAFKA_PORT, KAFKA_PORT);

            withEnv("LOG_DIR", "/tmp");
        }

        public SaslStrimziKafkaContainer() {
            this("latest-kafka-" + LATEST_KAFKA_VERSION);
        }

        @Override
        protected void doStart() {
            // we need it for the startZookeeper(); and startKafka(); to run container before...
            withCommand("sh", "-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
            super.doStart();
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);

            String command = "#!/bin/bash \n";
            command += "bin/zookeeper-server-start.sh config/zookeeper.properties &\n";
            command += "bin/kafka-server-start.sh config/server.properties" +
                    " --override listeners=SASL_PLAINTEXT://:" + KAFKA_PORT +
                    " --override advertised.listeners=" + getBootstrapServers() +
                    " --override zookeeper.connect=localhost:" + ZOOKEEPER_PORT;

            copyFileToContainer(
                    Transferable.of(command.getBytes(StandardCharsets.UTF_8), 700),
                    STARTER_SCRIPT);
        }

        public String getBootstrapServers() {
            return String.format("SASL_PLAINTEXT://%s:%s", getContainerIpAddress(), KAFKA_PORT);
        }
    }

}
