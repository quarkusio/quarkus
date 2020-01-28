package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that registers a listener which gets notified as soon as the CDI bean container is initialized.
 * This is a convenient way to get access to beans and configure them as soon as the container is started.
 * An instance of the running {@link BeanContainer} is provided to the listener.
 *
 * @see BeanContainerListener
 */
public final class BeanContainerListenerBuildItem extends MultiBuildItem {

    private final BeanContainerListener beanContainerListener;

    public BeanContainerListenerBuildItem(BeanContainerListener beanContainerListener) {
        this.beanContainerListener = beanContainerListener;
    }

    public BeanContainerListener getBeanContainerListener() {
        return beanContainerListener;
    }
}
