package io.quarkus.kubernetes.spi;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that wraps around Configurator objects.
 * The purpose of those build items is influence the configuration that will be feed to the generator process.
 * ConfigurationRegistry are similar to decorators, but are applied to configuration instead of generated resources.
 */
public final class ConfiguratorBuildItem extends MultiBuildItem {

    /**
     * The configurator
     */
    private final Object configurator;

    public ConfiguratorBuildItem(Object configurator) {
        this.configurator = configurator;
    }

    public Object getConfigurator() {
        return this.configurator;
    }

    public boolean matches(Class type) {
        return type.isInstance(configurator);

    }

    public <C> Optional<C> getConfigurator(Class<C> type) {
        if (matches(type)) {
            return Optional.<C> of((C) configurator);
        }
        return Optional.empty();
    }
}
