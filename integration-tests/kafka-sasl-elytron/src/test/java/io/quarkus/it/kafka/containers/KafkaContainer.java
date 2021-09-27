package io.quarkus.it.kafka.containers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.strimzi.StrimziKafkaContainer;

public class KafkaContainer extends FixedHostPortGenericContainer<KafkaContainer> {

    private static final Logger LOGGER = Logger.getLogger(KafkaContainer.class);

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";
    private static final int KAFKA_PORT = 9092;
    private static final String LATEST_KAFKA_VERSION;

    private static final List<String> supportedKafkaVersions = new ArrayList<>(3);

    static {
        InputStream inputStream = StrimziKafkaContainer.class.getResourceAsStream("/kafka-versions.txt");
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

    public KafkaContainer() {
        super("quay.io/strimzi/kafka:" + "latest-kafka-" + LATEST_KAFKA_VERSION);

        withExposedPorts(KAFKA_PORT);
        withFixedExposedPort(KAFKA_PORT, KAFKA_PORT);
        withCopyFileToContainer(MountableFile.forClasspathResource("kafkaServer.properties"),
                "/opt/kafka/config/server.properties");
        withCopyFileToContainer(MountableFile.forClasspathResource("krb5KafkaBroker.conf"), "/etc/krb5.conf");
        withFileSystemBind("src/test/resources/kafkabroker.keytab", "/opt/kafka/config/kafkabroker.keytab", BindMode.READ_ONLY);
        waitingFor(Wait.forLogMessage(".*Kafka startTimeMs:.*", 1));
        withNetwork(Network.SHARED);
        withEnv("LOG_DIR", "/tmp");
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
        LOGGER.info("Kafka servers :: " + getBootstrapServers());
        String command = "#!/bin/bash \n";
        command += "bin/zookeeper-server-start.sh config/zookeeper.properties &\n";
        command += "bin/kafka-server-start.sh config/server.properties" +
                " --override listeners=SASL_PLAINTEXT://:" + KAFKA_PORT +
                " --override advertised.listeners=" + getBootstrapServers();

        copyFileToContainer(Transferable.of(command.getBytes(StandardCharsets.UTF_8), 700), STARTER_SCRIPT);
    }

    public String getBootstrapServers() {
        return String.format("SASL_PLAINTEXT://%s:%s", getHost(), KAFKA_PORT);
    }

}
