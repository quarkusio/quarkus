package io.quarkus.datasource.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.component.ComponentLookup;

/**
 * Represents a queryable source of information regarding
 * which datasource can reasonably be {@link DataSourceRequestBuildItem requested}.
 * <p>
 * Extensions can consume this build item early on (before {@link DataSourceRequestBuildItem} is processed)
 * if they intend to consume a certain datasource only if there is a chance for it to exist.
 * This is useful in particular to create a component relying on the default datasource
 * only if it can be implicitly configured.
 */
public final class DataSourceLookupBuildItem extends SimpleBuildItem {
    private final ComponentLookup aggregatedLookup;

    public DataSourceLookupBuildItem(ComponentLookup aggregatedLookup) {
        this.aggregatedLookup = aggregatedLookup;
    }

    public ComponentLookup getLookup() {
        return aggregatedLookup;
    }
}
