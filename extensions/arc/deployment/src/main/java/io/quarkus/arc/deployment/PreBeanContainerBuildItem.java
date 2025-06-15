package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents the fully initialized CDI bean container. This item is produced immediately before
 * {@link BeanContainerBuildItem} in order to give recorders the chance to do something immediately before real
 * recording steps come into play.
 */
public final class PreBeanContainerBuildItem extends SimpleBuildItem {

    private final BeanContainer value;

    public PreBeanContainerBuildItem(BeanContainer value) {
        this.value = value;
    }

    public BeanContainer getValue() {
        return value;
    }
}
