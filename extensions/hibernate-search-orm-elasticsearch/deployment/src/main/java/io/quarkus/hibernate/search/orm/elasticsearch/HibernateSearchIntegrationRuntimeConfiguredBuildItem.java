package io.quarkus.hibernate.search.orm.elasticsearch;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;

public class HibernateSearchIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {
    private final String integrationName;
    private final String persistenceUnitName;
    private final HibernateOrmIntegrationRuntimeInitListener initListener;

    public HibernateSearchIntegrationRuntimeConfiguredBuildItem(String integrationName, String persistenceUnitName,
            HibernateOrmIntegrationRuntimeInitListener initListener) {
        if (integrationName == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.integrationName = integrationName;
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
        this.initListener = initListener;
    }

    @Override
    public String toString() {
        return HibernateSearchIntegrationRuntimeConfiguredBuildItem.class.getSimpleName() + " [" + integrationName + "]";
    }

    public HibernateOrmIntegrationRuntimeInitListener getInitListener() {
        return initListener;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }
}
