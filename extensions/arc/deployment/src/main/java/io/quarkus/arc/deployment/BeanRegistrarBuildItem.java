package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.builder.item.MultiBuildItem;

public final class BeanRegistrarBuildItem extends MultiBuildItem {

    private final BeanRegistrar beanRegistrar;

    public BeanRegistrarBuildItem(BeanRegistrar beanRegistrar) {
        this.beanRegistrar = beanRegistrar;
    }

    public BeanRegistrar getBeanRegistrar() {
        return beanRegistrar;
    }
}
