package io.quarkus.it.kafka;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.utility.MountableFile;

import io.quarkus.it.kafka.containers.KeycloakContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaKeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = Logger.getLogger(KafkaKeycloakTestResource.class);
    private StrimziKafkaContainer kafka;
    private KeycloakContainer keycloak;

    @Override
    public Map<String, String> start() {

        Map<String, String> properties = new HashMap<>();

        //Start keycloak container
        keycloak = new KeycloakContainer();
        keycloak.start();
        log.info(keycloak.getLogs());
        keycloak.createHostsFile();

        //Start kafka container
        this.kafka = new StrimziKafkaContainer("quay.io/strimzi/kafka:latest-kafka-3.0.0")
                .withBrokerId(1)
                .withKafkaConfigurationMap(Map.of("listener.security.protocol.map", "JWT:SASL_PLAINTEXT,BROKER1:PLAINTEXT"))
                .withNetworkAliases("kafka")
                .withServerProperties(MountableFile.forClasspathResource("kafkaServer.properties"))
                .withBootstrapServers(
                        c -> String.format("JWT://%s:%s", c.getHost(), c.getMappedPort(KAFKA_PORT)));
        this.kafka.start();
        log.info(this.kafka.getLogs());
        properties.put("kafka.bootstrap.servers", this.kafka.getBootstrapServers());

        return properties;
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
        if (keycloak != null) {
            keycloak.stop();
        }
    }
}
