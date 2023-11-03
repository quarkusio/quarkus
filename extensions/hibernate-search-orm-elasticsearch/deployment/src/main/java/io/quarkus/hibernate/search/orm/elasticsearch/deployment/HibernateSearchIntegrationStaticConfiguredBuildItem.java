package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;

public final class HibernateSearchIntegrationStaticConfiguredBuildItem extends MultiBuildItem {
    private final String integrationName;
    private final String persistenceUnitName;
    private final HibernateOrmIntegrationStaticInitListener initListener;
    private boolean xmlMappingRequired = false;

    public HibernateSearchIntegrationStaticConfiguredBuildItem(String integrationName, String persistenceUnitName,
            HibernateOrmIntegrationStaticInitListener initListener) {
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
        return HibernateSearchIntegrationStaticConfiguredBuildItem.class.getSimpleName() + " [" + integrationName + "]";
    }

    public HibernateOrmIntegrationStaticInitListener getInitListener() {
        return initListener;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateSearchIntegrationStaticConfiguredBuildItem setXmlMappingRequired(boolean xmlMappingRequired) {
        this.xmlMappingRequired = xmlMappingRequired;
        return this;
    }

    public boolean isXmlMappingRequired() {
        return xmlMappingRequired;
    }
}
