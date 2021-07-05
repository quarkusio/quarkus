package org.acme.quickstart.lra;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;

import io.narayana.lra.LRAConstants;
import io.narayana.lra.client.NarayanaLRAClient;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class LRAParticipantTestResourceLifecycle implements QuarkusTestResourceLifecycleManager {
    private static final Logger log = Logger.getLogger(LRAParticipantTestResourceLifecycle.class);
    private static GenericContainer<?> registry;
    private static String coordinatorEndpoint;

    // the endpoint on wich an LRA coordinator is listening
    public static String getCoordinatorEndpoint() {
        return coordinatorEndpoint;
    }

    @Override
    public Map<String, String> start() {
        registry = new GenericContainer<>("jbosstm/lra-coordinator:5.12.0.Final")
                .withExposedPorts(8080)
                .withEnv("QUARKUS_PROFILE", "prod");
        registry.start();

        Map<String, String> properties = new HashMap<>();

        coordinatorEndpoint = String.format("http://%s:%d/%s",
                registry.getContainerIpAddress(),
                registry.getFirstMappedPort(),
                LRAConstants.COORDINATOR_PATH_NAME);

        // the LRA implementation relies on a JAX-RS filter to manage interactions with a coordinator:
        System.setProperty(NarayanaLRAClient.LRA_COORDINATOR_URL_KEY, coordinatorEndpoint);

        properties.put(NarayanaLRAClient.LRA_COORDINATOR_URL_KEY, coordinatorEndpoint);

        log.infof("%s=%s%n", NarayanaLRAClient.LRA_COORDINATOR_URL_KEY, coordinatorEndpoint);

        return properties;
    }

    @Override
    public void stop() {
        String logs = registry.getLogs();
        registry.stop();
        log.infof("logs:%n%s%n", logs);
    }
}
