package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.builder.item.MultiBuildItem;

public final class BeanContainerListenerBuildItem extends MultiBuildItem {

    private final BeanContainerListener beanContainerListener;

    public BeanContainerListenerBuildItem(BeanContainerListener beanContainerListener) {
        this.beanContainerListener = beanContainerListener;
    }

    public BeanContainerListener getBeanContainerListener() {
        return beanContainerListener;
    }
}
