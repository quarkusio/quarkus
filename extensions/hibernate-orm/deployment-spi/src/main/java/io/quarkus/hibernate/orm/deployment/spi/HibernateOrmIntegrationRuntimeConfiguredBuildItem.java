package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationRuntimeInitListener;

/**
 * Registers a third-party integration that participates in the Hibernate ORM runtime-init bootstrap.
 * <p>
 * Produced by extensions that need to hook into the Hibernate ORM bootstrap at runtime
 * (e.g. Hibernate Search, Hibernate Reactive).
 * Each item targets a specific persistence unit and may carry a
 * {@link HibernateOrmIntegrationRuntimeInitListener} to contribute runtime properties
 * and service initiators.
 * <p>
 * Consumed during the {@code RUNTIME_INIT} build step that sets up the persistence provider
 * and starts persistence units.
 *
 * @see HibernateOrmIntegrationRuntimeInitListener
 * @see HibernateOrmIntegrationStaticConfiguredBuildItem
 */
public final class HibernateOrmIntegrationRuntimeConfiguredBuildItem extends MultiBuildItem {

    private final String integrationName;
    private final String persistenceUnitName;
    private HibernateOrmIntegrationRuntimeInitListener initListener;

    /**
     * @param integrationName a unique identifier for the integration (e.g. {@code "hibernate-search-elasticsearch"})
     * @param persistenceUnitName the name of the persistence unit this integration targets
     */
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

    /**
     * Sets a listener that will be called during runtime init to contribute runtime properties
     * and service initiators.
     */
    public HibernateOrmIntegrationRuntimeConfiguredBuildItem setInitListener(
            HibernateOrmIntegrationRuntimeInitListener initListener) {
        this.initListener = initListener;
        return this;
    }
}
