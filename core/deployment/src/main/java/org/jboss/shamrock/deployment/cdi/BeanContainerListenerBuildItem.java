package org.jboss.shamrock.deployment.cdi;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.shamrock.runtime.cdi.BeanContainerListener;

public final class BeanContainerListenerBuildItem extends MultiBuildItem {

    private final BeanContainerListener beanContainerListener;

    public BeanContainerListenerBuildItem(BeanContainerListener beanContainerListener) {
        this.beanContainerListener = beanContainerListener;
    }

    public BeanContainerListener getBeanContainerListener() {
        return beanContainerListener;
    }
}
