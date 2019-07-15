package io.quarkus.axon.deployment;

import io.quarkus.builder.item.MultiBuildItem;

abstract class AxonBuildItem extends MultiBuildItem {
    private Class<?> axonAnnotatedClass;

    public AxonBuildItem(Class<?> axonAnnotatedClass) {
        this.axonAnnotatedClass = axonAnnotatedClass;
    }

    public Class<?> getAxonAnnotatedClass() {
        return axonAnnotatedClass;
    }
}
