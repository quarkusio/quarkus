package io.quarkus.hibernate.orm.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Internal model to hold the mapping linking a JPA entity to its corresponding persistence units.
 */
public final class JpaModelPersistenceUnitMappingBuildItem extends SimpleBuildItem {

    private final Map<String, Set<String>> entityToPersistenceUnits;

    public JpaModelPersistenceUnitMappingBuildItem(Map<String, Set<String>> entityToPersistenceUnits) {
        this.entityToPersistenceUnits = Collections.unmodifiableMap(entityToPersistenceUnits);
    }

    public Map<String, Set<String>> getEntityToPersistenceUnits() {
        return entityToPersistenceUnits;
    }
}
