package io.quarkus.resteasy.common.deployment;

import org.jboss.resteasy.spi.InjectorFactory;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Gives access to the configured {@link InjectorFactory}
 * Can also be used as a marker indicating the RESTEasy injection has been properly set up
 */
public final class ResteasyInjectionReadyBuildItem extends SimpleBuildItem {

    private final RuntimeValue<InjectorFactory> injectorFactory;

    public ResteasyInjectionReadyBuildItem(RuntimeValue<InjectorFactory> injectorFactory) {
        this.injectorFactory = injectorFactory;
    }

    public RuntimeValue<InjectorFactory> getInjectorFactory() {
        return injectorFactory;
    }
}
