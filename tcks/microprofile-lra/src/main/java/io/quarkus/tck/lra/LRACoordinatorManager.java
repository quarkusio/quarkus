package io.quarkus.tck.lra;

import java.io.IOException;
import java.net.ServerSocket;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class LRACoordinatorManager {

    private static final int DEFAULT_PRECEDENCE = -100;
    private static final Logger LOGGER = Logger.getLogger(LRACoordinatorManager.class);
    private final int coordinatorPort = getFreePort(50000, 60000);

    private GenericContainer coordinatorContainer;

    public void beforeClass(
            @Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.BeforeSuite event) {
        LOGGER.debug("Starting LRA coordinator on port " + coordinatorPort);
        coordinatorContainer = new GenericContainer<>(DockerImageName.parse("jbosstm/lra-coordinator:latest"))
                // lra-coordinator is a Quarkus service
                .withEnv("QUARKUS_HTTP_PORT", String.valueOf(coordinatorPort))
                // need to run with host network because coordinator calls the TCK services from the container
                .withNetworkMode("host");

        coordinatorContainer.start();

        System.setProperty("lra.coordinator.url", String.format("http://localhost:%d/lra-coordinator", coordinatorPort));
    }

    public void afterClass(
            @Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.AfterSuite event) {
        if (coordinatorContainer != null && coordinatorContainer.isRunning()) {
            coordinatorContainer.stop();
        }
    }

    public int getFreePort(int from, int to) {
        int currentPort = from;
        while (currentPort <= to) {
            if (isLocalPortFree(currentPort)) {
                return currentPort;
            } else {
                currentPort++;
            }
        }

        throw new RuntimeException(
                String.format("Unable to find a free port for the LRA coordinator in range [%d, %d]", from, to));
    }

    private boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
