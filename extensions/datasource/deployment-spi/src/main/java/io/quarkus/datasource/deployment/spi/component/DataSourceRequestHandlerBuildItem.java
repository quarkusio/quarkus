package io.quarkus.datasource.deployment.spi.component;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.component.ComponentLookup;
import io.quarkus.runtime.util.ProgrammingParadigm;

/**
 * Declares an extension can handle {@link DataSourceRequestBuildItem},
 * and provides in particular a {@link ComponentLookup} to check for unavailable datasources,
 * so that other extensions can check what can be requested.
 * <p>
 * Should not be consumed except by the "common" datasource extension;
 * other extensions should consume {@link DataSourceLookupBuildItem}.
 */
public final class DataSourceRequestHandlerBuildItem extends MultiBuildItem {
    private final ProgrammingParadigm paradigm;
    private final ComponentLookup lookup;

    public DataSourceRequestHandlerBuildItem(ProgrammingParadigm paradigm, ComponentLookup lookup) {
        this.paradigm = paradigm;
        this.lookup = lookup;
    }

    public ProgrammingParadigm getParadigm() {
        return paradigm;
    }

    public ComponentLookup getLookup() {
        return lookup;
    }
}
