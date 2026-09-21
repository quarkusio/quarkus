package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Maps each JPA entity class name to the set of persistence units it belongs to.
 * <p>
 * Produced once all persistence unit descriptors have been built, after JPA model discovery
 * and per-persistence-unit assignment are complete.
 * <p>
 * Consumed by extensions that need to know which persistence unit owns a given entity
 * (e.g. Panache, Jakarta Data) to generate the correct persistence-unit-aware repository
 * or active-record implementation.
 */
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
