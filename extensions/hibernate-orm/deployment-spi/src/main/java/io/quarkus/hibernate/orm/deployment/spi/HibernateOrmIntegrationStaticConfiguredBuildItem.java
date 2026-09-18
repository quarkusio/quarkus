package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationStaticInitListener;

public final class HibernateOrmIntegrationStaticConfiguredBuildItem extends MultiBuildItem {

    private final String integrationName;
    private final String persistenceUnitName;
    private HibernateOrmIntegrationStaticInitListener initListener;
    private boolean xmlMappingRequired = false;

    public HibernateOrmIntegrationStaticConfiguredBuildItem(String integrationName, String persistenceUnitName) {
        if (integrationName == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.integrationName = integrationName;
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public String toString() {
        return HibernateOrmIntegrationStaticConfiguredBuildItem.class.getSimpleName() + " [" + integrationName + "]";
    }

    public String getIntegrationName() {
        return integrationName;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateOrmIntegrationStaticInitListener getInitListener() {
        return initListener;
    }

    public HibernateOrmIntegrationStaticConfiguredBuildItem setInitListener(
            HibernateOrmIntegrationStaticInitListener initListener) {
        this.initListener = initListener;
        return this;
    }

    public boolean isXmlMappingRequired() {
        return xmlMappingRequired;
    }

    public HibernateOrmIntegrationStaticConfiguredBuildItem setXmlMappingRequired(boolean xmlMappingRequired) {
        this.xmlMappingRequired = xmlMappingRequired;
        return this;
    }
}
