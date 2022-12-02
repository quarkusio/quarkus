package io.quarkus.resteasy.reactive.common.deployment;

import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class ParameterContainersBuildItem extends MultiBuildItem {

    private final Set<DotName> classNames;

    public ParameterContainersBuildItem(Set<DotName> classNames) {
        this.classNames = classNames;
    }

    public Set<DotName> getClassNames() {
        return classNames;
    }
}
