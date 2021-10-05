package io.quarkus.it.kafka;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.it.kafka.containers.KafkaContainer;
import io.quarkus.it.kafka.containers.KeycloakContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaKeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = Logger.getLogger(KafkaKeycloakTestResource.class);
    private KafkaContainer kafka;
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
        kafka = new KafkaContainer();
        kafka.start();
        log.info(kafka.getLogs());
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());

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
