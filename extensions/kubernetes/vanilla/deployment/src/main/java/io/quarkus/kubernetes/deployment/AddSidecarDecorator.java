package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.adapter.ContainerAdapter;
import io.dekorate.kubernetes.config.Container;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;

/**
 * Copied from dekorate in order to fix some issues.
 * TODO: This decorator should be removed and replaced by the Dekorate AddSidecarDecorator class after
 * https://github.com/dekorateio/dekorate/pull/1234 is merged and Dekorate is bumped.
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
        var sidecarContainer = ContainerAdapter.adapt(container);
        // This is necessary because of the issue that this pull request fixes https://github.com/dekorateio/dekorate/pull/1234
        sidecarContainer.setWorkingDir(container.getWorkingDir());
        podSpec.addToContainers(sidecarContainer);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }

}
