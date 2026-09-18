package io.quarkus.hibernate.orm.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Internal model to hold the mapping linking a JPA entity to its corresponding persistence units.
 *
 * @deprecated Use {@link io.quarkus.hibernate.orm.deployment.spi.JpaModelPersistenceUnitMappingBuildItem} instead.
 */
@Deprecated(since = "4.0", forRemoval = true)
public final class JpaModelPersistenceUnitMappingBuildItem extends SimpleBuildItem {

    private final Map<String, Set<String>> entityToPersistenceUnits;
    private final boolean incomplete;

    public JpaModelPersistenceUnitMappingBuildItem(Map<String, Set<String>> entityToPersistenceUnits, boolean incomplete) {
        this.entityToPersistenceUnits = Collections.unmodifiableMap(entityToPersistenceUnits);
        this.incomplete = incomplete;
    }

    public Map<String, Set<String>> getEntityToPersistenceUnits() {
        return entityToPersistenceUnits;
    }

    public boolean isIncomplete() {
        return incomplete;
    }
}
