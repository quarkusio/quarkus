package io.quarkus.arc.deployment;

import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * An extension can provide a custom {@link CurrentContextFactory}.
 */
public final class CurrentContextFactoryBuildItem extends SimpleBuildItem {

    private final RuntimeValue<CurrentContextFactory> factory;

    public CurrentContextFactoryBuildItem(RuntimeValue<CurrentContextFactory> factory) {
        this.factory = factory;
    }

    public RuntimeValue<CurrentContextFactory> getFactory() {
        return factory;
    }

}
