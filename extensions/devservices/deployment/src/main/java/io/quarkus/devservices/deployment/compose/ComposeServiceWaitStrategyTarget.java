package io.quarkus.devservices.deployment.compose;

import static io.quarkus.devservices.common.Labels.DOCKER_COMPOSE_CONTAINER_NUMBER;
import static io.quarkus.devservices.common.Labels.DOCKER_COMPOSE_SERVICE;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

/**
 * A WaitStrategyTarget that represents a container in a docker-compose file.
 */
public class ComposeServiceWaitStrategyTarget implements WaitStrategyTarget, Supplier<InspectContainerResponse> {

    private final Container container;

    private final DockerClient dockerClient;

    private final List<Integer> exposedPorts;

    private final AtomicReference<InspectContainerResponse> containerInfo = new AtomicReference<>();

    public ComposeServiceWaitStrategyTarget(DockerClient dockerClient, Container container) {
        this.dockerClient = dockerClient;
        this.container = container;
        this.exposedPorts = Arrays.stream(container.getPorts())
                .filter(port -> port.getPublicPort() != null)
                .map(ContainerPort::getPrivatePort)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Integer> getExposedPorts() {
        return exposedPorts;
    }

    @Override
    public String getContainerId() {
        return this.container.getId();
    }

    public String getServiceName() {
        return this.container.getLabels().get(DOCKER_COMPOSE_SERVICE);
    }

    public int getContainerNumber() {
        return Integer.parseInt(this.container.getLabels().get(DOCKER_COMPOSE_CONTAINER_NUMBER));
    }

    public String getContainerName() {
        return String.format("%s-%s", getServiceName(), getContainerNumber());
    }

    @Override
    public InspectContainerResponse getContainerInfo() {
        InspectContainerResponse value = this.containerInfo.get();
        if (value == null) {
            synchronized (this.containerInfo) {
                value = this.containerInfo.get();
                if (value == null) {
                    value = this.dockerClient.inspectContainerCmd(this.getContainerId()).exec();
                    this.containerInfo.set(value);
                }
            }
        }

        return value;
    }

    @Override
    public InspectContainerResponse get() {
        return getContainerInfo();
    }
}
