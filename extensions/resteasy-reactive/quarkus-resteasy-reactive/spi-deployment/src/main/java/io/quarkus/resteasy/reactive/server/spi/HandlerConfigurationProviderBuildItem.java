package io.quarkus.resteasy.reactive.server.spi;

import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build time that allows extensions to register a way to provide a value
 * for configuration that is provided at runtime and that is needed by
 * implementations of {@link org.jboss.resteasy.reactive.server.spi.GenericRuntimeConfigurableServerRestHandler}.
 *
 * Extensions are meant to create these build items by passing the configuration class as the first constructor
 * argument, and using a recorder to return a {@link Supplier} that will provide a value of that class as the
 * second argument constructor.
 *
 * Ideally we would have used generic to make things more type safe, but generics cannot be used in build items.
 */
@SuppressWarnings("rawtypes")
public final class HandlerConfigurationProviderBuildItem extends MultiBuildItem {

    /**
     * The runtime configuration class
     */
    private final Class configClass;

    /**
     * A supplier of the runtime value of the configuration class.
     * This supplier is meant to be provided by a recorder
     */
    private final Supplier valueSupplier;

    public HandlerConfigurationProviderBuildItem(Class configClass, Supplier valueSupplier) {
        this.configClass = configClass;
        this.valueSupplier = valueSupplier;
    }

    public Class getConfigClass() {
        return configClass;
    }

    public Supplier getValueSupplier() {
        return valueSupplier;
    }
}
