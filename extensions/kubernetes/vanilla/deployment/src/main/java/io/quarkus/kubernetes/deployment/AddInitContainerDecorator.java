package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.adapter.ContainerAdapter;
import io.dekorate.kubernetes.config.Container;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;

public abstract class AddInitContainerDecorator extends NamedResourceDecorator<PodSpecBuilder> {

    private final Container container;

    public AddInitContainerDecorator(String deployment, Container container) {
        super(deployment);
        this.container = container;
    }

    @Override
    public void andThenVisit(PodSpecBuilder podSpec, ObjectMeta resourceMeta) {
        var resource = ContainerAdapter.adapt(container);
        if (podSpec.hasMatchingInitContainer(this::hasInitContainer)) {
            update(podSpec, resource);
        } else {
            add(podSpec, resource);
        }
    }

    private void add(PodSpecBuilder podSpec, io.fabric8.kubernetes.api.model.Container resource) {
        podSpec.addToInitContainers(resource);
    }

    private void update(PodSpecBuilder podSpec, io.fabric8.kubernetes.api.model.Container resource) {
        var matching = podSpec.editMatchingInitContainer(this::hasInitContainer);
        if (resource.getImage() != null) {
            matching.withImage(resource.getImage());
        }

        if (resource.getImage() != null) {
            matching.withImage(resource.getImage());
        }

        if (resource.getWorkingDir() != null) {
            matching.withWorkingDir(resource.getWorkingDir());
        }

        if (resource.getCommand() != null && !resource.getCommand().isEmpty()) {
            matching.withCommand(resource.getCommand());
        }

        if (resource.getArgs() != null && !resource.getArgs().isEmpty()) {
            matching.withArgs(resource.getArgs());
        }

        if (resource.getReadinessProbe() != null) {
            matching.withReadinessProbe(resource.getReadinessProbe());
        }

        if (resource.getLivenessProbe() != null) {
            matching.withLivenessProbe(resource.getLivenessProbe());
        }

        matching.addAllToEnv(resource.getEnv());
        if (resource.getPorts() != null && !resource.getPorts().isEmpty()) {
            matching.withPorts(resource.getPorts());
        }

        matching.endInitContainer();
    }

    private boolean hasInitContainer(ContainerBuilder containerBuilder) {
        return containerBuilder.getName().equals(container.getName());
    }
}
