package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.runtime.cdi.BeanContainer;

public final class BeanContainerBuildItem extends SimpleBuildItem {

    private final BeanContainer value;

    public BeanContainerBuildItem(BeanContainer value) {
        this.value = value;
    }

    public BeanContainer getValue() {
        return value;
    }
}
