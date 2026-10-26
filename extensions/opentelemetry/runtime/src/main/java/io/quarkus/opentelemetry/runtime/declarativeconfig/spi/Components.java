package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.Set;

import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ExtendedDeclarativeConfigProperties;

// Resolves the components a Quarkus provider names in its nested nodes, as upstream resolves extension nodes
final class Components {

    private Components() {
    }

    // The one key of node, which names a component
    static String name(DeclarativeConfigProperties node, String description) {
        Set<String> keys = node == null ? Set.of() : node.getPropertyKeys();
        if (keys.size() != 1) {
            throw new DeclarativeConfigException(
                    "The %s must define exactly one component, found: %s".formatted(description, keys));
        }
        return keys.iterator().next();
    }

    // The configuration of the component name names in node. Nested nodes are plain properties; upstream components
    // such as the OTLP exporters need the extended type, and read SDK settings from its ConfigProvider.
    static DeclarativeConfigProperties config(DeclarativeConfigProperties parent, DeclarativeConfigProperties node,
            String name) {
        return new QuarkusExtendedConfigProperties(node.getStructured(name, DeclarativeConfigProperties.empty()),
                parent instanceof ExtendedDeclarativeConfigProperties extended ? extended.getConfigProvider()
                        : ConfigProvider.noop());
    }

    // The component the first provider of type and name creates, or null when there is none
    static Object create(ComponentLoader componentLoader, Class<?> type, String name,
            DeclarativeConfigProperties config) {
        for (ComponentProvider provider : componentLoader.load(ComponentProvider.class)) {
            if (type == provider.getType() && name.equals(provider.getName())) {
                return provider.create(config);
            }
        }
        return null;
    }

    static DeclarativeConfigException noSuchComponent(Class<?> type, String name) {
        return new DeclarativeConfigException(
                "No %s named '%s' was found. Ensure the artifact providing it is a dependency of the application."
                        .formatted(type.getSimpleName(), name));
    }
}
