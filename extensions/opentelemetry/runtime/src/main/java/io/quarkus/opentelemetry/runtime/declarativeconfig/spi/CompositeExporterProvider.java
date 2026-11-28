package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

// The QUARKUS_COMPOSITE exporter of one signal: resolves each exporter of the node's "exporters" list by name,
// applies the customizer to each one, as autoconfigure customized each named exporter, and composes the results.
// When one fails, the exporters already built are closed, as autoconfigure closed them.
abstract class CompositeExporterProvider<T extends AutoCloseable> implements ComponentProvider {

    private final Class<T> type;
    private final UnaryOperator<T> customizer;

    CompositeExporterProvider(Class<T> type, UnaryOperator<T> customizer) {
        this.type = type;
        this.customizer = customizer;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_COMPOSITE;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        List<DeclarativeConfigProperties> children = config.getStructuredList("exporters");
        if (children == null || children.isEmpty()) {
            throw new DeclarativeConfigException(
                    "The %s %s requires at least one exporter".formatted(QuarkusOTelModelBuilder.QUARKUS_COMPOSITE,
                            type.getSimpleName()));
        }
        ComponentLoader componentLoader = config.getComponentLoader();
        String description = QuarkusOTelModelBuilder.QUARKUS_COMPOSITE + " " + type.getSimpleName() + "'s exporter";
        List<T> built = new ArrayList<>();
        List<T> exporters = new ArrayList<>();
        try {
            for (DeclarativeConfigProperties child : children) {
                String name = Components.name(child, description);
                T exporter = type.cast(Components.create(componentLoader, type, name,
                        Components.config(config, child, name)));
                if (exporter == null) {
                    throw Components.noSuchComponent(type, name);
                }
                built.add(exporter);
                T customized = customizer.apply(exporter);
                if (customized != exporter) {
                    built.add(customized);
                }
                exporters.add(customized);
            }
        } catch (RuntimeException e) {
            for (int i = built.size() - 1; i >= 0; i--) {
                try {
                    built.get(i).close();
                } catch (Exception closeFailure) {
                    e.addSuppressed(closeFailure);
                }
            }
            throw e;
        }
        return compose(exporters);
    }

    // The exporters behind one exporter; a single exporter is returned as-is
    abstract T compose(List<T> exporters);
}
