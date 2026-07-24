package io.quarkus.narayana.lra.deployment.devservice;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.narayana.lra.deployment.devservice.DevServicesLRAProcessor.DEV_SERVICE_LABEL;

import java.util.OptionalInt;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.Labels;
import io.quarkus.runtime.LaunchMode;

public class LRACoordinatorContainer extends GenericContainer<LRACoordinatorContainer> implements Startable {

    private final Integer fixedExposedPort;

    private int exposedPort = -1;

    public LRACoordinatorContainer(DockerImageName imageName, Integer fixedExposedPort, String defaultNetworkId,
            boolean useSharedNetwork) {
        super(imageName);
        this.fixedExposedPort = fixedExposedPort;
        ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "lra-coordinator");
        Labels.addPortConfigLabel(this,
                fixedExposedPort != null && fixedExposedPort > 0 ? OptionalInt.of(fixedExposedPort) : OptionalInt.empty());
        waitingFor(Wait.forLogMessage(".*lra-coordinator-quarkus.*started in.*", 1));
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse inspectContainerResponse, boolean reused) {
        super.containerIsStarting(inspectContainerResponse, reused);
        this.exposedPort = getMappedPort(DevServicesLRAProcessor.LRA_COORDINATOR_CONTAINER_PORT);
    }

    @Override
    public void configure() {
        super.configure();

        addExposedPort(DevServicesLRAProcessor.LRA_COORDINATOR_CONTAINER_PORT);

        if (fixedExposedPort != null) {
            addFixedExposedPort(fixedExposedPort, DevServicesLRAProcessor.LRA_COORDINATOR_CONTAINER_PORT);
        }
    }

    @Override
    public String getConnectionInfo() {
        return String.format("http://%s:%d/lra-coordinator", getHost(), getExposedPort());
    }

    @Override
    public void close() {
        super.close();
    }

    public int getExposedPort() {
        return exposedPort;
    }

    public LRACoordinatorContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
        return configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);
    }
}
