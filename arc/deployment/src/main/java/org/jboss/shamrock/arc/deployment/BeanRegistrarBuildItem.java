package org.jboss.shamrock.arc.deployment;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.protean.arc.processor.BeanRegistrar;

public final class BeanRegistrarBuildItem extends MultiBuildItem {

    private final BeanRegistrar beanRegistrar;

    public BeanRegistrarBuildItem(BeanRegistrar beanRegistrar) {
        this.beanRegistrar = beanRegistrar;
    }

    public BeanRegistrar getBeanRegistrar() {
        return beanRegistrar;
    }
}
