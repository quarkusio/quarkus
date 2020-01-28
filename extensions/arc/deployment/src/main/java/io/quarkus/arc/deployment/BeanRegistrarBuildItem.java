package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used for registration of a synthetic bean; grants access to {@link BeanRegistrar} which is an API allowing to
 * specify a synthetic bean through series of configuration methods (scope, type, qualifiers, ...).
 *
 * This is a build time alternative to CDI BeanConfigurator API.
 */
public final class BeanRegistrarBuildItem extends MultiBuildItem {

    private final BeanRegistrar beanRegistrar;

    public BeanRegistrarBuildItem(BeanRegistrar beanRegistrar) {
        this.beanRegistrar = beanRegistrar;
    }

    public BeanRegistrar getBeanRegistrar() {
        return beanRegistrar;
    }
}
