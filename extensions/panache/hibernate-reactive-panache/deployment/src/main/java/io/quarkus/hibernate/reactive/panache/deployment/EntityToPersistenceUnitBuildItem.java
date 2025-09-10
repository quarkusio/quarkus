package io.quarkus.hibernate.reactive.panache.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to record that a specific JPA entity is associated with a specific persistence unit
 */
public final class EntityToPersistenceUnitBuildItem extends MultiBuildItem {

    private final String entityClass;
    private final String persistenceUnitName;

    public EntityToPersistenceUnitBuildItem(String entityClass, String persistenceUnitName) {
        this.entityClass = entityClass;
        this.persistenceUnitName = persistenceUnitName;
    }

    public String getEntityClass() {
        return entityClass;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }
}
