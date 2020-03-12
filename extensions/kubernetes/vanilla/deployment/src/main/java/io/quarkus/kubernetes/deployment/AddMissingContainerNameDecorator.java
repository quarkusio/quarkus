package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ContainerFluent;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImageDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;

public class AddMissingContainerNameDecorator extends ApplicationContainerDecorator<ContainerFluent> {

    private final String name;

    public AddMissingContainerNameDecorator(String deploymentName, String name) {
        super(deploymentName, null);
        this.name = name;
    }

    public void andThenVisit(ContainerFluent container) {
        if (container.getName() == null || container.getName().isEmpty()) {
            container.withName(this.name);
        }
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class, AddInitContainerDecorator.class,
                ApplyImageDecorator.class };
    }

    @Override
    public Class<? extends Decorator>[] before() {
        return new Class[] { ApplyContainerImageDecorator.class };
    }

}
