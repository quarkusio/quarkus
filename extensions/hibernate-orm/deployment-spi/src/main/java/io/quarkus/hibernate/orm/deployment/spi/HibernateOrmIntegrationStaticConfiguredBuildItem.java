package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationStaticInitListener;

/**
 * Registers a third-party integration that participates in the Hibernate ORM static-init bootstrap.
 * <p>
 * Produced by extensions that need to hook into the Hibernate ORM bootstrap at build time
 * (e.g. Hibernate Envers, Hibernate Search, Hibernate Reactive).
 * Each item targets a specific persistence unit and may carry a
 * {@link HibernateOrmIntegrationStaticInitListener} to contribute boot properties
 * and react to metadata initialization.
 * <p>
 * Consumed during the {@code STATIC_INIT} build step that builds
 * {@code QuarkusPersistenceUnitDefinition}s and records metadata initialization.
 *
 * @see HibernateOrmIntegrationStaticInitListener
 * @see HibernateOrmIntegrationRuntimeConfiguredBuildItem
 */
public final class HibernateOrmIntegrationStaticConfiguredBuildItem extends MultiBuildItem {

    private final String integrationName;
    private final String persistenceUnitName;
    private HibernateOrmIntegrationStaticInitListener initListener;
    private boolean xmlMappingRequired = false;

    /**
     * @param integrationName a unique identifier for the integration (e.g. {@code "hibernate-envers"})
     * @param persistenceUnitName the name of the persistence unit this integration targets
     */
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

    /**
     * Sets a listener that will be called during static init to contribute boot properties
     * and react to metadata initialization.
     */
    public HibernateOrmIntegrationStaticConfiguredBuildItem setInitListener(
            HibernateOrmIntegrationStaticInitListener initListener) {
        this.initListener = initListener;
        return this;
    }

    public boolean isXmlMappingRequired() {
        return xmlMappingRequired;
    }

    /**
     * Indicates that this integration requires XML mapping to be enabled for its persistence unit.
     */
    public HibernateOrmIntegrationStaticConfiguredBuildItem setXmlMappingRequired(boolean xmlMappingRequired) {
        this.xmlMappingRequired = xmlMappingRequired;
        return this;
    }
}
