package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImageDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ContainerFluent;

/**
 * A decorator for applying an image to a container capable of overriding the internal {@link ApplyImageDecorator}.
 */
public class ApplyContainerImageDecorator extends ApplicationContainerDecorator<ContainerFluent> {
    private final String image;

    public ApplyContainerImageDecorator(String containerName, String image) {
        super((String) null, containerName);
        this.image = image;
    }

    public ApplyContainerImageDecorator(String deploymentName, String containerName, String image) {
        super(deploymentName, containerName);
        this.image = image;
    }

    public void andThenVisit(ContainerFluent container) {
        container.withImage(this.image);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class, AddInitContainerDecorator.class,
                ApplyImageDecorator.class };
    }

}
