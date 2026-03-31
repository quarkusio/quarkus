package io.quarkus.kafka.client.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.kafka.client.deployment.DevServicesKafkaProcessor.DEV_SERVICE_LABEL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.runtime.LaunchMode;

public class KafkaContainer extends GenericContainer<KafkaContainer> implements Startable {

    private static final String DEFAULT_INTERNAL_TOPIC_RF = "1";

    private static final String DEFAULT_CLUSTER_ID = "4L6g3nShT-eMCtK--X86sw";

    static final int KAFKA_PORT = 9092;

    static final String STARTER_SCRIPT = "/tmp/testcontainers_start.sh";

    static final String[] COMMAND = {
            "sh",
            "-c",
            "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT,
    };

    static final WaitStrategy WAIT_STRATEGY = Wait.forLogMessage(".*Transitioning from RECOVERY to RUNNING.*", 1);

    private final String hostName;
    private final boolean useSharedNetwork;

    static Map<String, String> envVars() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("CLUSTER_ID", DEFAULT_CLUSTER_ID);

        envVars.put("KAFKA_LISTENERS",
                "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9093,CONTROLLER://0.0.0.0:9094");
        envVars.put(
                "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT");
        envVars.put("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
        envVars.put("KAFKA_PROCESS_ROLES", "broker,controller");
        envVars.put("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER");

        envVars.put("KAFKA_NODE_ID", "1");

        String controllerQuorumVoters = String.format("%s@localhost:9094", envVars.get("KAFKA_NODE_ID"));
        envVars.put("KAFKA_CONTROLLER_QUORUM_VOTERS", controllerQuorumVoters);

        envVars.put("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        envVars.put("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", DEFAULT_INTERNAL_TOPIC_RF);
        envVars.put("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        envVars.put("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", DEFAULT_INTERNAL_TOPIC_RF);
        envVars.put("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        envVars.put("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");

        // Kafka Queues default config
        envVars.put("KAFKA_GROUP_COORDINATOR_REBALANCE_PROTOCOLS", "classic,consumer,share");
        envVars.put("KAFKA_UNSTABLE_API_VERSIONS_ENABLE", "true");
        envVars.put("KAFKA_SHARE_COORDINATOR_STATE_TOPIC_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        return envVars;
    }

    public KafkaContainer(DockerImageName dockerImageName, String defaultNetworkId, boolean useSharedNetwork) {
        super(dockerImageName);
        this.useSharedNetwork = useSharedNetwork;

        withExposedPorts(KAFKA_PORT);
        withEnv(envVars());

        withCommand(COMMAND);
        waitingFor(WAIT_STRATEGY);

        this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "kafka");
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        String brokerAdvertisedListener = String.format(
                "BROKER://%s:%s", containerInfo.getConfig().getHostName(),
                "9093");
        List<String> advertisedListeners = new ArrayList<>();
        advertisedListeners.add("PLAINTEXT://" + getBootstrapServers());
        advertisedListeners.add(brokerAdvertisedListener);

        String kafkaAdvertisedListeners = String.join(",", advertisedListeners);

        String command = "#!/bin/bash\n";
        // exporting KAFKA_ADVERTISED_LISTENERS with the container hostname
        command += String.format("export KAFKA_ADVERTISED_LISTENERS=%s\n", kafkaAdvertisedListeners);

        command += "/etc/kafka/docker/run \n";
        copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
    }

    public KafkaContainer withPort(int fixedPort) {
        if (fixedPort < 0) {
            throw new IllegalArgumentException("The fixed Kafka port must be greater than 0");
        } else if (fixedPort > 0) {
            addFixedExposedPort(fixedPort, 9092);
        }
        return this;
    }

    public KafkaContainer withEnv(Map<String, String> envVars) {
        super.withEnv(envVars);
        return this;
    }

    public String getEffectiveHostName() {
        return useSharedNetwork ? hostName : getHost();
    }

    public int getEffectivePort() {
        return useSharedNetwork ? 9092 : getMappedPort(KAFKA_PORT);
    }

    public String getBootstrapServers() {
        return String.format("%s:%s", getEffectiveHostName(), getEffectivePort());
    }

    @Override
    public String getConnectionInfo() {
        return getBootstrapServers();
    }

    public KafkaContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
        return configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);
    }
}
