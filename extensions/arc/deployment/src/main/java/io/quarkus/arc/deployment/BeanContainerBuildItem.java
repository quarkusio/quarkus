package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents the fully initialized CDI bean container.
 * This item is produced as the last step of the ArC bootstrap process.
 */
public final class BeanContainerBuildItem extends SimpleBuildItem {

    private final BeanContainer value;

    public BeanContainerBuildItem(BeanContainer value) {
        this.value = value;
    }

    public BeanContainer getValue() {
        return value;
    }
}
