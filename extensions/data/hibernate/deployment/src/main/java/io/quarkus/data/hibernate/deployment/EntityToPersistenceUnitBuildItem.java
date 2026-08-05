package io.quarkus.data.hibernate.deployment;

import io.quarkus.builder.item.MultiBuildItem;

// FIXME: duplicate with ORM and probably HR
/**
 * Used to record that a specific JPA entity is associated with a specific persistence unit
 */
public final class EntityToPersistenceUnitBuildItem extends MultiBuildItem
        implements Comparable<EntityToPersistenceUnitBuildItem> {

    private final String entityClass;
    private final String persistenceUnitName;
    private final String reactivePersistenceUnitName;

    public EntityToPersistenceUnitBuildItem(String entityClass, String persistenceUnitName,
            String reactivePersistenceUnitName) {
        this.entityClass = entityClass;
        this.persistenceUnitName = persistenceUnitName;
        this.reactivePersistenceUnitName = reactivePersistenceUnitName;
    }

    public String getEntityClass() {
        return entityClass;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public String getReactivePersistenceUnitName() {
        return reactivePersistenceUnitName;
    }

    @Override
    public int compareTo(EntityToPersistenceUnitBuildItem other) {
        // No tie-breaker is needed because Quarkus Data supports
        // at most one persistence unit of each type per entity.
        return entityClass.compareTo(other.entityClass);
    }
}
