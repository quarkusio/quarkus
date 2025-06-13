package io.quarkus.kafka.client.deployment;

import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devservices.common.ConfigureUtil;

public class KafkaNativeContainer extends GenericContainer<KafkaNativeContainer> implements Startable {

    private static final String STARTER_SCRIPT = "/work/run.sh";

    private final Integer fixedExposedPort;
    private final boolean useSharedNetwork;

    private String additionalArgs = null;
    private int exposedPort = -1;

    private final String hostName;

    public KafkaNativeContainer(DockerImageName dockerImageName, int fixedExposedPort, String defaultNetworkId,
            boolean useSharedNetwork) {
        super(dockerImageName);
        this.fixedExposedPort = fixedExposedPort;
        this.useSharedNetwork = useSharedNetwork;
        String cmd = String.format("while [ ! -f %s ]; do sleep 0.1; done; sleep 0.1; %s", STARTER_SCRIPT, STARTER_SCRIPT);
        withCommand("sh", "-c", cmd);
        waitingFor(Wait.forLogMessage(".*Kafka broker started.*", 1));
        this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "kafka");
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

        // docker exec since docker cp doesn't work with kubedock yet
        try {
            execInContainer("sh", "-c",
                    String.format("echo -e \"%1$s\" >> %2$s && chmod 777 %2$s", cmd, STARTER_SCRIPT));
        } catch (Exception e) {
            throw new RuntimeException("Can't create run script in the Kafka native container.", e);
        }
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

        if (fixedExposedPort != null) {
            addFixedExposedPort(fixedExposedPort, DevServicesKafkaProcessor.KAFKA_PORT);
        }
    }

    public String getBootstrapServers() {
        return getKafkaAdvertisedListeners();
    }

    @Override
    public String getConnectionInfo() {
        return getBootstrapServers();
    }

    @Override
    public void close() {
        super.close();
    }
}
