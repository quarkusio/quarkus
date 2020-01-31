package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used for registration of a synthetic observer; grants access to {@link ObserverRegistrar} which is an API allowing to
 * specify a synthetic observer through series of configuration methods.
 *
 * This is a build time alternative to CDI ObserverMethodConfigurator API.
 */
public final class ObserverRegistrarBuildItem extends MultiBuildItem {

    private final ObserverRegistrar observerRegistrar;

    public ObserverRegistrarBuildItem(ObserverRegistrar observerRegistrar) {
        this.observerRegistrar = observerRegistrar;
    }

    public ObserverRegistrar getObserverRegistrar() {
        return observerRegistrar;
    }

}
