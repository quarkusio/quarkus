package io.quarkus.kafka.client.deployment;

import static io.quarkus.kafka.client.deployment.DevServicesKafkaProcessor.DEV_SERVICE_LABEL;

import java.util.Map;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.runtime.LaunchMode;

/**
 * Container for official Apache Kafka images with support for shared networks.
 */
public class OfficialKafkaContainer extends KafkaContainer implements Startable {

    private final Integer fixedExposedPort;
    private final boolean useSharedNetwork;
    private final String hostName;

    public OfficialKafkaContainer(DockerImageName dockerImageName, int fixedExposedPort, String defaultNetworkId,
            boolean useSharedNetwork) {
        super(dockerImageName);
        this.fixedExposedPort = fixedExposedPort;
        this.useSharedNetwork = useSharedNetwork;
        this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "kafka");
    }

    public OfficialKafkaContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
        withLabel(DEV_SERVICE_LABEL, serviceName);
        if (LaunchMode.DEVELOPMENT == launchMode) {
            withLabel("quarkus-dev-service", serviceName);
        }
        return this;
    }

    @Override
    public OfficialKafkaContainer withEnv(Map<String, String> env) {
        super.withEnv(env);
        return this;
    }

    @Override
    protected void configure() {
        super.configure();

        if (fixedExposedPort != null && fixedExposedPort != 0) {
            addFixedExposedPort(fixedExposedPort, DevServicesKafkaProcessor.KAFKA_PORT);
        }

        // Configure advertised listeners for shared network
        if (useSharedNetwork) {
            // Override the default advertised listeners to use the container hostname
            // This allows other containers in the shared network to connect
            withEnv("KAFKA_LISTENERS",
                    String.format("PLAINTEXT://0.0.0.0:%d,BROKER://localhost:9093",
                            DevServicesKafkaProcessor.KAFKA_PORT));
            withEnv("KAFKA_ADVERTISED_LISTENERS",
                    String.format("PLAINTEXT://%s:%d,BROKER://localhost:9093",
                            hostName, DevServicesKafkaProcessor.KAFKA_PORT));
            withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:PLAINTEXT,BROKER:PLAINTEXT");
            withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
        }
    }

    @Override
    public String getBootstrapServers() {
        if (useSharedNetwork) {
            // In shared network mode, return the container hostname
            return String.format("%s:%d", hostName, DevServicesKafkaProcessor.KAFKA_PORT);
        }
        // Otherwise, use the default behavior (docker host + mapped port)
        return super.getBootstrapServers();
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
