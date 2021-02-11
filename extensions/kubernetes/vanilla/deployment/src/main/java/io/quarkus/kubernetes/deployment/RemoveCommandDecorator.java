package io.quarkus.kubernetes.deployment;

import java.util.List;

import io.dekorate.deps.kubernetes.api.model.ContainerFluent;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;

public class RemoveCommandDecorator extends ApplicationContainerDecorator<ContainerFluent<?>> {

    public RemoveCommandDecorator(String deploymentName, String containerName) {
        super(deploymentName, containerName);
    }

    @Override
    public void andThenVisit(ContainerFluent<?> container) {
        container.withCommand((List) null);
        container.withArgs((List) null);
    }

}
