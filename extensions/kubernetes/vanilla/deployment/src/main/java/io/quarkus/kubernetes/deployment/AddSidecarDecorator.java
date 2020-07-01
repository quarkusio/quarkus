package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.kubernetes.api.model.PodSpecBuilder;
import io.dekorate.kubernetes.config.Container;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;

/**
 * Copied from dekorate in order to fix some issues
 */
class AddSidecarDecorator extends NamedResourceDecorator<PodSpecBuilder> {

    private final Container container;

    public AddSidecarDecorator(Container container) {
        this(ANY, container);
    }

    public AddSidecarDecorator(String deployment, Container container) {
        super(deployment);
        this.container = container;
    }

    @Override
    public void andThenVisit(PodSpecBuilder podSpec, ObjectMeta resourceMeta) {
        // this was changed to use our patched adapter
        podSpec.addToContainers(ContainerAdapter.adapt(container));
    }

    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }

}
