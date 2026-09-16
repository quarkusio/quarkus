package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationRuntimeInitListener;

public final class HibernateOrmIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {

    private final String integrationName;
    private final String persistenceUnitName;
    private HibernateOrmIntegrationRuntimeInitListener initListener;

    public HibernateOrmIntegrationRuntimeConfiguredBuildItem(String integrationName, String persistenceUnitName) {
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
        return HibernateOrmIntegrationRuntimeConfiguredBuildItem.class.getSimpleName() + " [" + integrationName + "]";
    }

    public String getIntegrationName() {
        return integrationName;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public HibernateOrmIntegrationRuntimeInitListener getInitListener() {
        return initListener;
    }

    public HibernateOrmIntegrationRuntimeConfiguredBuildItem setInitListener(
            HibernateOrmIntegrationRuntimeInitListener initListener) {
        this.initListener = initListener;
        return this;
    }
}
