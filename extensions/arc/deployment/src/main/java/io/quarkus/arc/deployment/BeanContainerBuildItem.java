package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.builder.item.SimpleBuildItem;

public final class BeanContainerBuildItem extends SimpleBuildItem {

    private final BeanContainer value;

    public BeanContainerBuildItem(BeanContainer value) {
        this.value = value;
    }

    public BeanContainer getValue() {
        return value;
    }
}
