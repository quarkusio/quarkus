package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import org.hibernate.SessionFactory;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.Key;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;

public final class HibernateSearchIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {
    private final String integrationName;
    @Key(SessionFactory.class)
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
