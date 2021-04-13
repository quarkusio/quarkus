package io.quarkus.it.kafka.streams;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaSSLTestResource implements QuarkusTestResourceLifecycleManager {

    private static final SSLStrimziKafkaContainer kafka = new SSLStrimziKafkaContainer()
            .withCopyFileToContainer(MountableFile.forClasspathResource("server.properties"),
                    "/opt/kafka/config/server.properties")
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

    static class SSLStrimziKafkaContainer extends GenericContainer<SSLStrimziKafkaContainer> {

        private static final Logger LOGGER = Logger.getLogger(SSLStrimziKafkaContainer.class.getName());

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

        public SSLStrimziKafkaContainer(final String version) {
            super("quay.io/strimzi/kafka:" + version);
            super.withNetwork(Network.SHARED);

            // exposing kafka port from the container
            withExposedPorts(KAFKA_PORT);

            withEnv("LOG_DIR", "/tmp");
        }

        public SSLStrimziKafkaContainer() {
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

            kafkaExposedPort = getMappedPort(KAFKA_PORT);

            StringBuilder advertisedListeners = new StringBuilder(getBootstrapServers());

            Collection<ContainerNetwork> cns = containerInfo.getNetworkSettings().getNetworks().values();

            for (ContainerNetwork cn : cns) {
                advertisedListeners.append("," + "BROKER://").append(cn.getIpAddress()).append(":9093");
            }

            String command = "#!/bin/bash \n";
            command += "bin/zookeeper-server-start.sh config/zookeeper.properties &\n";
            command += "bin/kafka-server-start.sh config/server.properties --override listeners=BROKER://0.0.0.0:9093,SSL://0.0.0.0:"
                    + KAFKA_PORT +
                    " --override advertised.listeners=" + advertisedListeners.toString() +
                    " --override zookeeper.connect=localhost:" + ZOOKEEPER_PORT +
                    " --override listener.security.protocol.map=BROKER:PLAINTEXT,SSL:SSL" +
                    " --override inter.broker.listener.name=BROKER\n";

            copyFileToContainer(
                    Transferable.of(command.getBytes(StandardCharsets.UTF_8), 700),
                    STARTER_SCRIPT);
        }

        public String getBootstrapServers() {
            return String.format("SSL://%s:%s", getContainerIpAddress(), kafkaExposedPort);
        }
    }

}
