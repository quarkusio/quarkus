package io.quarkus.kafka.client.deployment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.devservices.common.ConfigureUtil;

public class KafkaNativeContainer extends GenericContainer<KafkaNativeContainer> {

    private static final String STARTER_SCRIPT = "/work/run.sh";

    private final Integer fixedExposedPort;
    private final boolean useSharedNetwork;

    private String additionalArgs = null;
    private int exposedPort = -1;

    private String hostName = null;

    public KafkaNativeContainer(DockerImageName dockerImageName, int fixedExposedPort, String serviceName,
            boolean useSharedNetwork) {
        super(dockerImageName);
        this.fixedExposedPort = fixedExposedPort;
        this.useSharedNetwork = useSharedNetwork;
        if (serviceName != null) {
            withLabel(DevServicesKafkaProcessor.DEV_SERVICE_LABEL, serviceName);
        }
        String cmd = String.format("while [ ! -f %s ]; do sleep 0.1; done; sleep 0.1; %s", STARTER_SCRIPT, STARTER_SCRIPT);
        withCommand("sh", "-c", cmd);
        waitingFor(Wait.forLogMessage(".*Kafka broker started.*", 1));
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarting(containerInfo, reused);
        // Set exposed port
        this.exposedPort = getMappedPort(DevServicesKafkaProcessor.KAFKA_PORT);
        // follow output
        // Start and configure the advertised address
        String cmd = "#!/bin/bash\n";
        cmd += "/work/kafka";
        cmd += " -Dkafka.advertised.listeners=" + getBootstrapServers();
        if (useSharedNetwork) {
            cmd += " -Dkafka.listeners=BROKER://:9093,PLAINTEXT://:9092,CONTROLLER://:9094";
            cmd += " -Dkafka.interbroker.listener.name=BROKER";
            cmd += " -Dkafka.controller.listener.names=CONTROLLER";
            cmd += " -Dkafka.listener.security.protocol.map=BROKER:PLAINTEXT,CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT";
            cmd += " -Dkafka.early.start.listeners=BROKER,CONTROLLER,PLAINTEXT";
        }
        if (additionalArgs != null) {
            cmd += " " + additionalArgs;
        }

        //noinspection OctalInteger
        copyFileToContainer(
                Transferable.of(cmd.getBytes(StandardCharsets.UTF_8), 0777),
                STARTER_SCRIPT);
    }

    private String getKafkaAdvertisedListeners() {
        List<String> addresses = new ArrayList<>();
        if (useSharedNetwork) {
            addresses.add(String.format("BROKER://%s:9093", hostName));
        }
        // See https://github.com/quarkusio/quarkus/issues/21819
        // Kafka is always exposed to the Docker host network
        addresses.add(String.format("PLAINTEXT://%s:%d", getHost(), getExposedKafkaPort()));
        return String.join(",", addresses);
    }

    public int getExposedKafkaPort() {
        return exposedPort;
    }

    @Override
    protected void configure() {
        super.configure();

        addExposedPort(DevServicesKafkaProcessor.KAFKA_PORT);
        hostName = ConfigureUtil.configureSharedNetwork(this, "kafka");

        if (fixedExposedPort != null) {
            addFixedExposedPort(fixedExposedPort, DevServicesKafkaProcessor.KAFKA_PORT);
        }
    }

    public String getBootstrapServers() {
        return getKafkaAdvertisedListeners();
    }

}
