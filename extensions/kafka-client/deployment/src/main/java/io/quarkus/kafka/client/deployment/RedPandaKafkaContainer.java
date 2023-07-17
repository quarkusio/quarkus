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

/**
 * Container configuring and starting the Redpanda broker.
 * See https://vectorized.io/docs/quick-start-docker/
 */
final class RedPandaKafkaContainer extends GenericContainer<RedPandaKafkaContainer> {

    private final Integer fixedExposedPort;
    private final boolean useSharedNetwork;
    private final RedPandaBuildTimeConfig redpandaConfig;

    private String hostName = null;

    private static final String STARTER_SCRIPT = "/var/lib/redpanda/redpanda.sh";

    RedPandaKafkaContainer(DockerImageName dockerImageName, int fixedExposedPort, String serviceName,
            boolean useSharedNetwork, RedPandaBuildTimeConfig redpandaConfig) {
        super(dockerImageName);
        this.fixedExposedPort = fixedExposedPort;
        this.useSharedNetwork = useSharedNetwork;
        this.redpandaConfig = redpandaConfig;

        if (serviceName != null) { // Only adds the label in dev mode.
            withLabel(DevServicesKafkaProcessor.DEV_SERVICE_LABEL, serviceName);
        }

        // For redpanda, we need to start the broker - see https://vectorized.io/docs/quick-start-docker/
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("sh");
        });
        withCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; sleep 0.1; " +
                STARTER_SCRIPT);
        waitingFor(Wait.forLogMessage(".*Started Kafka API server.*", 1));
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarting(containerInfo, reused);

        // Start and configure the advertised address
        String command = "#!/bin/bash\n";
        command += "/usr/bin/rpk redpanda start --check=false --node-id 0 --smp 1 ";
        command += "--memory 1G --overprovisioned --reserve-memory 0M ";
        command += String.format("--kafka-addr %s ", getKafkaAddresses());
        command += String.format("--advertise-kafka-addr %s ", getKafkaAdvertisedAddresses());
        command += "--set redpanda.auto_create_topics_enabled=true ";
        if (redpandaConfig.transactionEnabled) {
            command += "--set redpanda.enable_idempotence=true ";
            command += "--set redpanda.enable_transactions=true ";
        }

        //noinspection OctalInteger
        copyFileToContainer(
                Transferable.of(command.getBytes(StandardCharsets.UTF_8), 0777),
                STARTER_SCRIPT);
    }

    private String getKafkaAddresses() {
        List<String> addresses = new ArrayList<>();
        if (useSharedNetwork) {
            addresses.add("PLAINTEXT://0.0.0.0:29092");
        }
        // See https://github.com/quarkusio/quarkus/issues/21819
        // Kafka is always available on the Docker host network
        addresses.add("OUTSIDE://0.0.0.0:9092");
        return String.join(",", addresses);
    }

    private String getKafkaAdvertisedAddresses() {
        List<String> addresses = new ArrayList<>();
        if (useSharedNetwork) {
            addresses.add(String.format("PLAINTEXT://%s:29092", hostName));
        }
        // See https://github.com/quarkusio/quarkus/issues/21819
        // Kafka is always exposed to the Docker host network
        addresses.add(String.format("OUTSIDE://%s:%d", getHost(), getMappedPort(DevServicesKafkaProcessor.KAFKA_PORT)));
        return String.join(",", addresses);
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
        return getKafkaAdvertisedAddresses();
    }

}
