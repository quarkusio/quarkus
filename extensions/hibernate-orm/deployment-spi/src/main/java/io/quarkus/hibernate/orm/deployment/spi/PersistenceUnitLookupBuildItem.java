package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.component.ComponentLookup;

/**
 * Provides a {@link ComponentLookup lookup} to determine
 * if a persistence unit can reasonably be {@link PersistenceUnitRequestBuildItem requested}.
 * <p>
 * Extensions can consume this build item early on (before {@link PersistenceUnitRequestBuildItem} is processed)
 * if they intend to consume a certain persistence unit only if there is a chance for it to exist.
 * This is useful in particular to create a component relying on the default datasource
 * only if it can be implicitly configured.
 */
public final class PersistenceUnitLookupBuildItem extends SimpleBuildItem {

    private final ComponentLookup aggregatedLookup;

    public PersistenceUnitLookupBuildItem(ComponentLookup aggregatedLookup) {
        this.aggregatedLookup = aggregatedLookup;
    }

    public ComponentLookup getLookup() {
        return aggregatedLookup;
    }

}
