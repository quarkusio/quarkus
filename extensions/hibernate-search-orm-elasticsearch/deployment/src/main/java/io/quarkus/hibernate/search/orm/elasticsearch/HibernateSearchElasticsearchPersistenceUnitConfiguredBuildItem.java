package io.quarkus.hibernate.search.orm.elasticsearch;

import io.quarkus.builder.item.MultiBuildItem;

public final class HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem extends MultiBuildItem {

    private final String persistenceUnitName;

    public HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(String persistenceUnitName) {
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + persistenceUnitName + "]";
    }
}
