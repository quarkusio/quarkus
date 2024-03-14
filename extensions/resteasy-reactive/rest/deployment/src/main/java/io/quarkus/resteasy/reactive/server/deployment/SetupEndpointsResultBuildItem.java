package io.quarkus.resteasy.reactive.server.deployment;

import java.util.List;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;

import io.quarkus.builder.item.SimpleBuildItem;

public final class SetupEndpointsResultBuildItem extends SimpleBuildItem {

    private final List<ResourceClass> resourceClasses;
    private final List<ResourceClass> subResourceClasses;
    private final AdditionalReaders additionalReaders;
    private final AdditionalWriters additionalWriters;

    public SetupEndpointsResultBuildItem(List<ResourceClass> resourceClasses, List<ResourceClass> subResourceClasses,
            AdditionalReaders additionalReaders, AdditionalWriters additionalWriters) {
        this.resourceClasses = resourceClasses;
        this.subResourceClasses = subResourceClasses;
        this.additionalReaders = additionalReaders;
        this.additionalWriters = additionalWriters;
    }

    public List<ResourceClass> getResourceClasses() {
        return resourceClasses;
    }

    public List<ResourceClass> getSubResourceClasses() {
        return subResourceClasses;
    }

    public AdditionalReaders getAdditionalReaders() {
        return additionalReaders;
    }

    public AdditionalWriters getAdditionalWriters() {
        return additionalWriters;
    }
}
